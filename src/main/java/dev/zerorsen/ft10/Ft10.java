package dev.zerorsen.ft10;

import dev.zerorsen.ft10.compat.ClientCompat;
import dev.zerorsen.ft10.config.ConfigManager;
import dev.zerorsen.ft10.config.Ft10Config;
import dev.zerorsen.ft10.config.ScoreStorage;
import dev.zerorsen.ft10.core.DeathMessageParser;
import dev.zerorsen.ft10.core.PlayerRef;
import dev.zerorsen.ft10.core.Session;
import dev.zerorsen.ft10.feedback.Notifier;
import dev.zerorsen.ft10.mc.DeathMessages;
import dev.zerorsen.ft10.mc.Players;
import dev.zerorsen.ft10.mc.ServerKey;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * The mod's runtime: owns the config, the stored scores and the {@link Session} of the current connection, and
 * forwards what happens in the game (hits, deaths, death messages) to that session.
 */
public final class Ft10 {
    private static Ft10 instance;

    private final ConfigManager config;
    private final ScoreStorage storage;
    private final Notifier notifier;
    private DeathMessageParser parser;
    private Session session;
    private final Set<UUID> deadPlayers = new HashSet<>();
    private Screen pendingScreen;

    private Ft10(Path configDir, Path gameDir) {
        this.config = new ConfigManager(configDir.resolve("ft10.json"));
        this.storage = new ScoreStorage(gameDir.resolve("ft10").resolve("scores.json"));
        this.notifier = new Notifier(this::config);
    }

    static Ft10 create(Path configDir, Path gameDir) {
        instance = new Ft10(configDir, gameDir);
        instance.config.load();
        instance.storage.load();
        instance.rebuildParser();
        return instance;
    }

    public static Ft10 get() {
        return instance;
    }

    public Ft10Config config() {
        return config.get();
    }

    /** Saves the config and applies changes that need rebuilding (death message patterns). */
    public void saveConfig() {
        config.save();
        rebuildParser();
    }

    public Optional<Session> session() {
        return Optional.ofNullable(session);
    }

    public DeathMessageParser parser() {
        return parser;
    }

    /** Opens a screen on the next tick (opening it from a chat command would be closed with the chat). */
    public void openScreenNextTick(Screen screen) {
        pendingScreen = screen;
    }

    // ---- lifecycle ------------------------------------------------------------------------------------------

    void onJoin(Minecraft minecraft) {
        PlayerRef self = minecraft.player != null
            ? Players.of(minecraft.player)
            : PlayerRef.named(minecraft.getUser().getName());
        String server = ServerKey.current(minecraft);
        session = new Session(self, storage.book(server), this::rules, notifier);
        deadPlayers.clear();
        Ft10Client.LOGGER.debug("FT10 session started on {} as {}", server, self.name());
    }

    void onDisconnect() {
        storage.flush(config().saveScores);
        session = null;
        deadPlayers.clear();
    }

    void onStopping() {
        storage.flush(config().saveScores);
    }

    void onTick(Minecraft minecraft) {
        Keybinds.tick(minecraft);
        if (pendingScreen != null) {
            ClientCompat.setScreen(minecraft, pendingScreen);
            pendingScreen = null;
        }
        long now = System.currentTimeMillis();
        if (session != null && minecraft.level != null && config().enabled) {
            scanDeaths(minecraft, minecraft.level, now);
            session.tick(now);
        }
        storage.tick(now, config().saveScores);
    }

    // ---- game signals ---------------------------------------------------------------------------------------

    /** A damage event packet: {@code victimId} was hurt, {@code causeId} is the attacker (-1 if none). */
    public void onDamage(int victimId, int causeId) {
        ClientLevel level = Minecraft.getInstance().level;
        if (!tracking() || level == null) return;
        Entity victim = level.getEntity(victimId);
        Entity cause = causeId >= 0 ? level.getEntity(causeId) : null;
        if (victim instanceof Player v && cause instanceof Player a) {
            session.onHit(Players.of(a), Players.of(v), System.currentTimeMillis());
        }
    }

    /** An entity-event "death" packet arrived; only other players' deaths matter here. */
    public void onEntityDeath(ClientboundEntityEventPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!tracking() || minecraft.level == null) return;
        if (packet.getEntity(minecraft.level) instanceof Player player && player != minecraft.player) {
            deadPlayers.add(player.getUUID());
            session.onDeathSeen(Players.of(player), System.currentTimeMillis());
        }
    }

    /** We swung at an entity (client side, before the server confirms damage). */
    public void onSwing(Entity target) {
        if (tracking() && target instanceof Player p) session.onSwing(Players.of(p), System.currentTimeMillis());
    }

    /** The server told a player they died; {@code message} is their death message. */
    public void onCombatKill(int playerId, Component message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!tracking() || minecraft.player == null || playerId != minecraft.player.getId()) return;
        long now = System.currentTimeMillis();
        Optional<DeathMessages.Death> death = DeathMessages.parse(message, parser);
        if (death.isPresent() && death.get().victim().sameAs(session.self())) {
            session.onDeathMessage(session.self(), death.get().killer(), now);
        } else {
            session.onDeathSeen(session.self(), now);
        }
    }

    /** A system (non-player) chat message. Death messages are read from here. */
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !tracking()) return;
        DeathMessages.parse(message, parser)
            .ifPresent(d -> session.onDeathMessage(d.victim(), d.killer(), System.currentTimeMillis()));
    }

    /** Other players' deaths show up as their health dropping to 0. */
    private void scanDeaths(Minecraft minecraft, ClientLevel level, long now) {
        Set<UUID> seen = new HashSet<>();
        for (AbstractClientPlayer player : level.players()) {
            if (player == minecraft.player) continue;
            UUID id = player.getUUID();
            seen.add(id);
            if (player.isDeadOrDying()) {
                if (deadPlayers.add(id)) session.onDeathSeen(Players.of(player), now);
            } else {
                deadPlayers.remove(id);
            }
        }
        deadPlayers.retainAll(seen);
    }

    private boolean tracking() {
        return session != null && config().enabled;
    }

    private Session.Rules rules() {
        Ft10Config c = config();
        return new Session.Rules(c.combatTimeoutSeconds * 1000L, c.switchLockSeconds * 1000L, c.switchOnHit,
            c.goal, c.autoNewSet);
    }

    private void rebuildParser() {
        Ft10Config c = config();
        parser = new DeathMessageParser(c.usePresetPatterns, c.customPatterns);
        parser.errors().forEach(error -> Ft10Client.LOGGER.warn("Ignoring death message pattern: {}", error));
    }
}

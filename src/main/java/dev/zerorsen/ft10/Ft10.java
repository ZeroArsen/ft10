package dev.zerorsen.ft10;

import dev.zerorsen.ft10.compat.ClientCompat;
import dev.zerorsen.ft10.config.ConfigManager;
import dev.zerorsen.ft10.config.Ft10Config;
import dev.zerorsen.ft10.config.ScoreStorage;
import dev.zerorsen.ft10.core.DeathMessageParser;
import dev.zerorsen.ft10.core.KillDetector;
import dev.zerorsen.ft10.core.PlayerRef;
import dev.zerorsen.ft10.core.RepeatGuard;
import dev.zerorsen.ft10.core.Rivalry;
import dev.zerorsen.ft10.core.ScoreUpdate;
import dev.zerorsen.ft10.core.Session;
import dev.zerorsen.ft10.core.StatWatcher;
import dev.zerorsen.ft10.feedback.Debug;
import dev.zerorsen.ft10.feedback.Notifier;
import dev.zerorsen.ft10.mc.DeathMessages;
import dev.zerorsen.ft10.mc.Players;
import dev.zerorsen.ft10.mc.ServerKey;
import dev.zerorsen.ft10.mc.Sidebar;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

/**
 * The mod's runtime: owns the config, the stored scores and the {@link Session} of the current connection, and
 * forwards what happens in the game (hits, deaths, kill messages, sidebar counters) to that session.
 */
public final class Ft10 {
    /** A repeated action bar or title counts again only after it was gone this long. */
    private static final long REPEAT_MS = 8_000;
    /** A subtitle this recent is read together with the title ("KILL!" + "Steve"). */
    private static final long SUBTITLE_PAIR_MS = 1_000;
    private static final int SIDEBAR_EVERY_TICKS = 5;
    private static final int MAX_MESSAGE_LENGTH = 512;

    private static final String CHAT = "ft10.debug.channel.chat";
    private static final String ACTION_BAR = "ft10.debug.channel.action_bar";
    private static final String TITLE = "ft10.debug.channel.title";
    private static final String SUBTITLE = "ft10.debug.channel.subtitle";
    private static final String DEATH_SCREEN = "ft10.debug.channel.death_screen";

    private static Ft10 instance;

    private final ConfigManager config;
    private final ScoreStorage storage;
    private final Notifier notifier;
    private final Debug debug = new Debug();
    private final RepeatGuard repeats = new RepeatGuard(REPEAT_MS);
    private final StatWatcher stats = new StatWatcher();
    private DeathMessageParser parser;
    private Session session;
    private final Set<UUID> deadPlayers = new HashSet<>();
    /** Last known game mode per player (tab list), to spot switches to spectator. */
    private final Map<UUID, GameType> gameModes = new HashMap<>();
    private Screen pendingScreen;
    private Component lastSubtitle;
    private long lastSubtitleAt;
    private int ticks;

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

    public Debug debug() {
        return debug;
    }

    /** Deletes every score on every server (settings button); returns how many. */
    public int resetAllScores() {
        return storage.resetEverything(config().saveScores);
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
        session = new Session(self, storage.book(server), this::rules, new SessionListener());
        resetSignals();
        Ft10Client.LOGGER.debug("FT10 session started on {} as {}", server, self.name());
    }

    void onDisconnect() {
        storage.flush(config().saveScores);
        session = null;
        resetSignals();
        // Only here, not on join: the tab list (with everyone's game mode) can arrive before the JOIN event.
        gameModes.clear();
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
            if (++ticks % SIDEBAR_EVERY_TICKS == 0) readSidebar(minecraft.level, now);
            session.tick(now);
        }
        storage.tick(now, config().saveScores);
    }

    private void resetSignals() {
        deadPlayers.clear();
        repeats.clear();
        stats.clear();
        lastSubtitle = null;
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
            deathSeen(Players.of(player), "ft10.debug.source.event", System.currentTimeMillis());
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
        Optional<DeathMessages.Death> death = DeathMessages.parse(message, parser, context(now));
        if (death.isPresent() && death.get().victim().sameAs(session.self())) {
            apply(death.get(), DEATH_SCREEN, now);
        } else {
            deathSeen(session.self(), "ft10.debug.source.death_screen", now);
        }
    }

    /** A system (non-player) chat message, or an action bar sent as one. */
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay) onActionBar(message);
        else readMessage(message, CHAT, System.currentTimeMillis());
    }

    public void onActionBar(Component message) {
        long now = System.currentTimeMillis();
        if (repeated(ACTION_BAR, message, now)) return;
        readMessage(message, ACTION_BAR, now);
    }

    public void onTitle(Component title) {
        long now = System.currentTimeMillis();
        Component subtitle = lastSubtitle != null && now - lastSubtitleAt <= SUBTITLE_PAIR_MS ? lastSubtitle : null;
        Component text = subtitle == null ? title : Component.empty().append(title).append(" ").append(subtitle);
        if (repeated(TITLE, text, now)) return;
        if (subtitle != null && read(title, TITLE, now, false)) return;
        readMessage(text, TITLE, now);
    }

    public void onSubtitle(Component subtitle) {
        long now = System.currentTimeMillis();
        lastSubtitle = subtitle;
        lastSubtitleAt = now;
        if (repeated(SUBTITLE, subtitle, now)) return;
        read(subtitle, SUBTITLE, now, false); // unrecognized subtitles are logged with their title
    }

    /**
     * Tab list updates. Duel servers often fake a death by switching the loser to spectator, with no death packet
     * at all: a switch to spectator by us, or by someone we're fighting, is a death we saw.
     */
    public void onPlayerInfoUpdate(ClientboundPlayerInfoUpdatePacket packet) {
        if (!packet.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE)) return;
        long now = System.currentTimeMillis();
        for (ClientboundPlayerInfoUpdatePacket.Entry entry : packet.entries()) {
            GameType before = gameModes.put(entry.profileId(), entry.gameMode());
            if (!tracking() || before == null || before == GameType.SPECTATOR) continue;
            if (entry.gameMode() != GameType.SPECTATOR) continue;
            PlayerRef self = session.self();
            Optional<PlayerRef> player = Players.byUuid(entry.profileId());
            if (isSelf(entry.profileId()) || player.map(self::sameAs).orElse(false)) {
                deathSeen(self, "ft10.debug.source.spectator", now);
                continue;
            }
            player.filter(p -> session.fighting(p, now))
                .ifPresent(p -> deathSeen(p, "ft10.debug.source.spectator", now));
        }
    }

    /** The session's own ref can lack a UUID (the player entity may not exist yet on JOIN), so ask the client too. */
    private boolean isSelf(UUID id) {
        Minecraft minecraft = Minecraft.getInstance();
        return id.equals(session.self().uuid()) || (minecraft.player != null && id.equals(minecraft.player.getUUID()));
    }

    /** Players left the tab list; with "count combat logs" on, leaving mid-fight is our kill. */
    public void onPlayersLeft(List<UUID> ids) {
        ids.forEach(gameModes::remove);
        if (!tracking()) return;
        long now = System.currentTimeMillis();
        for (UUID id : ids) {
            session.onPlayerLeft(id.toString(), now).ifPresent(p -> debug.signal("ft10.debug.left", p));
        }
    }

    // ---- reading signals ------------------------------------------------------------------------------------

    private void readMessage(Component message, String channel, long now) {
        read(message, channel, now, true);
    }

    /** Parses one message; true if it was a death. */
    private boolean read(Component message, String channel, long now, boolean reportUnrecognized) {
        if (!tracking()) return false;
        String text = message.getString();
        if (text.isBlank() || text.length() > MAX_MESSAGE_LENGTH) return false;
        Optional<DeathMessages.Death> death = DeathMessages.parse(message, parser, context(now));
        if (death.isPresent()) {
            apply(death.get(), channel, now);
            return true;
        }
        if (reportUnrecognized && aboutAFight(text, now)) debug.unrecognized(channel, DeathMessageParser.strip(text));
        return false;
    }

    private void apply(DeathMessages.Death death, String channel, long now) {
        Component where = Component.translatable(channel);
        if (death.killerKnown()) {
            debug.signal("ft10.debug.message", where, death.victim(), death.killer(), death.source());
            session.onDeathMessage(death.victim(), death.killer(), now);
        } else {
            debug.signal("ft10.debug.message_guess", where, death.victim(), death.source());
            session.onDeathSeen(death.victim(), now);
        }
    }

    private void deathSeen(PlayerRef victim, String sourceKey, long now) {
        if (victim.sameAs(session.self()) || session.fighting(victim, now)) {
            debug.signal("ft10.debug.death_seen", victim, Component.translatable(sourceKey));
        }
        session.onDeathSeen(victim, now);
    }

    /** Did this text name us or someone we just fought? Then a kill message may be hiding in it. */
    private boolean aboutAFight(String text, long now) {
        if (Players.mentions(text, session.self().name())) return true;
        for (PlayerRef opponent : session.recentOpponents(now)) {
            if (Players.mentions(text, opponent.name())) return true;
        }
        return false;
    }

    private boolean repeated(String channel, Component message, long now) {
        return !repeats.isNew(channel + '\u0000' + message.getString(), now);
    }

    /** Other players' deaths show up as their health dropping to 0. */
    private void scanDeaths(Minecraft minecraft, ClientLevel level, long now) {
        Set<UUID> seen = new HashSet<>();
        for (AbstractClientPlayer player : level.players()) {
            if (player == minecraft.player) continue;
            UUID id = player.getUUID();
            seen.add(id);
            if (player.isDeadOrDying()) {
                if (deadPlayers.add(id)) deathSeen(Players.of(player), "ft10.debug.source.health", now);
            } else {
                deadPlayers.remove(id);
            }
        }
        deadPlayers.retainAll(seen);
    }

    /** "Kills: 12" → "Kills: 13" in the sidebar, for servers that never really kill anyone. */
    private void readSidebar(ClientLevel level, long now) {
        Ft10Config c = config();
        if (!c.useSidebarStats) return;
        for (StatWatcher.Change change : stats.update(Sidebar.lines(level), c.statKillLabels, c.statDeathLabels)) {
            if (change.stat() == StatWatcher.Stat.KILLS) {
                Optional<PlayerRef> victim = session.onKillCounted(now);
                if (victim.isPresent()) debug.signal("ft10.debug.stat_kill", change.from(), change.to(), victim.get());
                else debug.signal("ft10.debug.stat_kill_nobody", change.from(), change.to());
            } else {
                debug.signal("ft10.debug.stat_death", change.from(), change.to());
                session.onDeathCounted(now);
            }
        }
    }

    private DeathMessages.Context context(long now) {
        Session s = session;
        return new DeathMessages.Context(s.self(), name -> s.recentOpponent(name, now), p -> s.fighting(p, now),
            () -> s.currentOpponent(now));
    }

    private boolean tracking() {
        return session != null && config().enabled;
    }

    private Session.Rules rules() {
        Ft10Config c = config();
        return new Session.Rules(c.combatTimeoutSeconds * 1000L, c.switchLockSeconds * 1000L, c.switchOnHit,
            c.goal, c.autoNewSet, c.countCombatLogs);
    }

    private void rebuildParser() {
        Ft10Config c = config();
        parser = new DeathMessageParser(c.usePresetPatterns, c.customPatterns);
        parser.errors().forEach(error -> Ft10Client.LOGGER.warn("Ignoring death message pattern: {}", error));
    }

    /** Points go to the notifier; settled deaths that involve us are explained in debug mode. */
    private final class SessionListener implements Session.Listener {
        @Override
        public void onScore(ScoreUpdate update) {
            notifier.onScore(update);
        }

        @Override
        public void onActiveChanged(Rivalry rivalry) {
            notifier.onActiveChanged(rivalry);
        }

        @Override
        public void onKill(KillDetector.Kill kill) {
            if (session == null) return;
            PlayerRef self = session.self();
            if (!self.sameAs(kill.victim()) && !self.sameAs(kill.killer())) return;
            if (kill.killer() == null) debug.signal("ft10.debug.kill_nobody", kill.victim());
            else debug.signal("ft10.debug.kill", kill.victim(), kill.killer());
        }
    }
}

package dev.zerorsen.ft10.mc;

import dev.zerorsen.ft10.compat.ProfileCompat;
import dev.zerorsen.ft10.compat.TextCompat;
import dev.zerorsen.ft10.core.DeathMessageParser;
import dev.zerorsen.ft10.core.PlayerRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Turns entities, names and chat components into {@link PlayerRef}s, using the tab list as the source of truth. */
public final class Players {
    private Players() {}

    public static PlayerRef of(Player player) {
        return PlayerRef.of(player.getUUID(), player.getScoreboardName());
    }

    /** An online player by name (case-insensitive), from the tab list. */
    public static Optional<PlayerRef> byName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        for (PlayerInfo info : online()) {
            String profileName = ProfileCompat.name(info.getProfile());
            if (profileName.equalsIgnoreCase(name)) return Optional.of(ref(info));
        }
        return Optional.empty();
    }

    public static Optional<PlayerRef> byUuid(UUID uuid) {
        ClientPacketListener connection = connection();
        if (connection == null || uuid == null) return Optional.empty();
        PlayerInfo info = connection.getPlayerInfo(uuid);
        return info == null ? Optional.empty() : Optional.of(ref(info));
    }

    /**
     * The player a chat component names, or empty when it names something else (a mob, an item). A
     * show_entity hover is trusted first; otherwise the text must match someone on the tab list.
     */
    public static Optional<PlayerRef> fromComponent(Component component) {
        Optional<TextCompat.HoverEntity> hover = TextCompat.hoverEntity(component.getStyle());
        if (hover.isPresent()) {
            TextCompat.HoverEntity entity = hover.get();
            if (!entity.isPlayer()) return Optional.empty();
            String fallbackName = entity.name() != null ? entity.name() : plain(component);
            return byUuid(entity.uuid()).or(() -> Optional.of(PlayerRef.of(entity.uuid(), fallbackName)));
        }

        String insertion = component.getStyle().getInsertion();
        if (insertion != null) {
            Optional<PlayerRef> byInsertion = byName(insertion);
            if (byInsertion.isPresent()) return byInsertion;
        }

        String text = plain(component);
        Optional<PlayerRef> exact = byName(text);
        if (exact.isPresent()) return exact;
        // Team prefixes/suffixes ("[Red] Alex ✦"): try each word.
        for (String word : text.split("\\s+")) {
            Optional<PlayerRef> match = byName(word.replaceAll("[^\\w.*]", ""));
            if (match.isPresent()) return match;
        }
        return Optional.empty();
    }

    /** Names of everyone on the tab list, for command suggestions. */
    public static List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (PlayerInfo info : online()) names.add(ProfileCompat.name(info.getProfile()));
        return names;
    }

    private static Iterable<PlayerInfo> online() {
        ClientPacketListener connection = connection();
        return connection == null ? List.of() : connection.getOnlinePlayers();
    }

    /** Null outside a world (and in unit tests, where there is no client). */
    private static ClientPacketListener connection() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft == null ? null : minecraft.getConnection();
    }

    private static PlayerRef ref(PlayerInfo info) {
        return PlayerRef.of(ProfileCompat.id(info.getProfile()), ProfileCompat.name(info.getProfile()));
    }

    private static String plain(Component component) {
        return DeathMessageParser.strip(component.getString());
    }
}

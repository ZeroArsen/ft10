package dev.zerorsen.ft10.core;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * A player as the client knows them. The UUID comes from the tab list or an entity; it can be missing when a
 * name only shows up in a chat message and the server hides the tab list.
 */
public record PlayerRef(UUID uuid, String name) {
    public PlayerRef {
        Objects.requireNonNull(name, "name");
    }

    public static PlayerRef of(UUID uuid, String name) {
        return new PlayerRef(uuid, name);
    }

    public static PlayerRef named(String name) {
        return new PlayerRef(null, name);
    }

    /** Stable key used to store scores: the UUID when known, otherwise the lower-case name. */
    public String key() {
        return uuid != null ? uuid.toString() : "name:" + name.toLowerCase(Locale.ROOT);
    }

    /** Same player? UUIDs win when both sides have one, names (case-insensitive) otherwise. */
    public boolean sameAs(PlayerRef other) {
        if (other == null) return false;
        if (uuid != null && other.uuid != null) return uuid.equals(other.uuid);
        return name.equalsIgnoreCase(other.name);
    }
}

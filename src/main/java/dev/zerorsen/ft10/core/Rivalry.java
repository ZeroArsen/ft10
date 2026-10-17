package dev.zerorsen.ft10.core;

import java.util.UUID;

/** The running score against one opponent on one server. */
public final class Rivalry {
    private final String key;
    private String name;
    private int me;
    private int them;
    private int setsMe;
    private int setsThem;
    private long lastFought;

    public Rivalry(String key, String name) {
        this.key = key;
        this.name = name;
    }

    public String key() { return key; }
    public String name() { return name; }
    public int me() { return me; }
    public int them() { return them; }
    public int setsMe() { return setsMe; }
    public int setsThem() { return setsThem; }
    public long lastFought() { return lastFought; }

    /** Players can rename; we always show the latest name we saw. */
    public void rename(String name) {
        if (name != null && !name.isBlank()) this.name = name;
    }

    public void setScore(int me, int them) {
        this.me = Math.max(0, me);
        this.them = Math.max(0, them);
    }

    public void setSets(int setsMe, int setsThem) {
        this.setsMe = Math.max(0, setsMe);
        this.setsThem = Math.max(0, setsThem);
    }

    public void touch(long now) {
        this.lastFought = Math.max(lastFought, now);
    }

    /** The opponent as a {@link PlayerRef}, rebuilt from the storage key. */
    public PlayerRef ref() {
        if (key.startsWith("name:")) return PlayerRef.named(name);
        try {
            return PlayerRef.of(UUID.fromString(key), name);
        } catch (IllegalArgumentException e) {
            return PlayerRef.named(name);
        }
    }

    public boolean hasSets() {
        return setsMe > 0 || setsThem > 0;
    }

    public void reset() {
        me = them = setsMe = setsThem = 0;
    }

    Snapshot snapshot() {
        return new Snapshot(key, me, them, setsMe, setsThem);
    }

    void restore(Snapshot s) {
        me = s.me();
        them = s.them();
        setsMe = s.setsMe();
        setsThem = s.setsThem();
    }

    record Snapshot(String key, int me, int them, int setsMe, int setsThem) {}
}

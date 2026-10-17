package dev.zerorsen.ft10.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Lets each text through once. Servers re-send action bars every tick and some repeat titles, so the same
 * "You killed Steve" can arrive dozens of times for one kill; a text counts again only after it was gone for
 * {@code windowMs}.
 */
public final class RepeatGuard {
    private final long windowMs;
    private final Map<String, Long> lastSeen = new HashMap<>();

    public RepeatGuard(long windowMs) {
        this.windowMs = windowMs;
    }

    /** True the first time {@code text} shows up (or the first time after a pause of {@code windowMs}). */
    public boolean isNew(String text, long now) {
        lastSeen.values().removeIf(at -> now - at > windowMs);
        return lastSeen.put(text, now) == null;
    }

    public void clear() {
        lastSeen.clear();
    }
}

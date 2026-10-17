package dev.zerorsen.ft10.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Remembers who last hit whom. This is the fallback kill credit when no death message names the killer,
 * and it tells whether we are still trading hits with someone.
 */
public final class CombatTracker {
    /** Entries older than this are dropped by {@link #prune}. */
    private static final long FORGET_AFTER_MS = 60_000;

    private record Hit(PlayerRef attacker, long at) {}

    private final PlayerRef self;
    private final Map<String, Hit> lastHitOn = new HashMap<>();
    private final Map<String, Long> lastExchange = new HashMap<>();

    public CombatTracker(PlayerRef self) {
        this.self = self;
    }

    /** {@code attacker} damaged {@code victim}. */
    public void recordHit(PlayerRef attacker, PlayerRef victim, long now) {
        lastHitOn.put(victim.key(), new Hit(attacker, now));
        if (self.sameAs(attacker)) lastExchange.put(victim.key(), now);
        else if (self.sameAs(victim)) lastExchange.put(attacker.key(), now);
    }

    /** We swung at {@code other}; counts as fighting even if the hit was blocked. */
    public void recordSwing(PlayerRef other, long now) {
        lastExchange.put(other.key(), now);
    }

    /** The player who last damaged {@code victim} within {@code windowMs}. */
    public Optional<PlayerRef> lastAttacker(PlayerRef victim, long now, long windowMs) {
        Hit hit = lastHitOn.get(victim.key());
        if (hit == null || now - hit.at() > windowMs) return Optional.empty();
        return Optional.of(hit.attacker());
    }

    /** Did we trade a hit with the player behind {@code key} within {@code windowMs}? */
    public boolean recentlyFought(String key, long now, long windowMs) {
        Long at = lastExchange.get(key);
        return at != null && now - at <= windowMs;
    }

    /** A player died: their fight is over, so their hits no longer count. */
    public void forget(PlayerRef player) {
        lastHitOn.remove(player.key());
        lastExchange.remove(player.key());
    }

    /** We died: every fight we were in is over. */
    public void forgetExchanges() {
        lastExchange.clear();
        lastHitOn.remove(self.key());
    }

    public void prune(long now) {
        lastHitOn.values().removeIf(hit -> now - hit.at() > FORGET_AFTER_MS);
        lastExchange.values().removeIf(at -> now - at > FORGET_AFTER_MS);
    }
}

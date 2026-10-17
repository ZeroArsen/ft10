package dev.zerorsen.ft10.core;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Remembers who last hit whom. This is the fallback kill credit when no death message names the killer,
 * and it tells whether we are still trading hits with someone.
 */
public final class CombatTracker {
    /** Entries older than this are dropped by {@link #prune}. */
    private static final long FORGET_AFTER_MS = 60_000;

    private record Hit(PlayerRef attacker, PlayerRef victim, long at) {}

    /** Our last hit with someone, either way round (or a swing at them). */
    private record Exchange(PlayerRef other, long at) {}

    private final PlayerRef self;
    private final Map<String, Hit> lastHitOn = new HashMap<>();
    private final Map<String, Exchange> lastExchange = new HashMap<>();
    private final Map<String, Exchange> lastSwing = new HashMap<>();

    public CombatTracker(PlayerRef self) {
        this.self = self;
    }

    /** {@code attacker} damaged {@code victim}. */
    public void recordHit(PlayerRef attacker, PlayerRef victim, long now) {
        lastHitOn.put(victim.key(), new Hit(attacker, victim, now));
        if (self.sameAs(attacker)) lastExchange.put(victim.key(), new Exchange(victim, now));
        else if (self.sameAs(victim)) lastExchange.put(attacker.key(), new Exchange(attacker, now));
    }

    /** We swung at {@code other}; counts as fighting even if the hit was blocked. */
    public void recordSwing(PlayerRef other, long now) {
        lastExchange.put(other.key(), new Exchange(other, now));
        lastSwing.put(other.key(), new Exchange(other, now));
    }

    /** The player who last damaged {@code victim} within {@code windowMs}. */
    public Optional<PlayerRef> lastAttacker(PlayerRef victim, long now, long windowMs) {
        Hit hit = lastHitOn.get(victim.key());
        if (hit == null || now - hit.at() > windowMs) return Optional.empty();
        return Optional.of(hit.attacker());
    }

    /**
     * The player we attacked most recently within {@code windowMs}: a confirmed hit, or a swing (someone else may
     * have hit our victim after us, which overwrites our hit). Players who only hit us don't count.
     */
    public Optional<PlayerRef> lastVictimOfSelf(long now, long windowMs) {
        Optional<Exchange> hit = lastHitOn.values().stream()
            .filter(h -> self.sameAs(h.attacker()) && now - h.at() <= windowMs)
            .map(h -> new Exchange(h.victim(), h.at()))
            .max(Comparator.comparingLong(Exchange::at));
        Optional<Exchange> swing = lastSwing.values().stream()
            .filter(e -> now - e.at() <= windowMs)
            .max(Comparator.comparingLong(Exchange::at));
        if (hit.isEmpty()) return swing.map(Exchange::other);
        if (swing.isEmpty()) return hit.map(Exchange::other);
        return Optional.of(hit.get().at() >= swing.get().at() ? hit.get().other() : swing.get().other());
    }

    /** Did we trade a hit with the player behind {@code key} within {@code windowMs}? */
    public boolean recentlyFought(String key, long now, long windowMs) {
        Exchange e = lastExchange.get(key);
        return e != null && now - e.at() <= windowMs;
    }

    /** The player behind {@code key} if we fought them within {@code windowMs}. */
    public Optional<PlayerRef> opponent(String key, long now, long windowMs) {
        Exchange e = lastExchange.get(key);
        return e != null && now - e.at() <= windowMs ? Optional.of(e.other()) : Optional.empty();
    }

    /** Everyone we fought within {@code windowMs}, most recent first. */
    public List<PlayerRef> recentOpponents(long now, long windowMs) {
        return lastExchange.values().stream()
            .filter(e -> now - e.at() <= windowMs)
            .sorted(Comparator.comparingLong(Exchange::at).reversed())
            .map(Exchange::other)
            .toList();
    }

    /** A player died: their fight is over, so their hits no longer count. */
    public void forget(PlayerRef player) {
        lastHitOn.remove(player.key());
        lastExchange.remove(player.key());
        lastSwing.remove(player.key());
    }

    /** We died: every fight we were in is over. */
    public void forgetExchanges() {
        lastExchange.clear();
        lastSwing.clear();
        lastHitOn.remove(self.key());
    }

    public void prune(long now) {
        lastHitOn.values().removeIf(hit -> now - hit.at() > FORGET_AFTER_MS);
        lastExchange.values().removeIf(e -> now - e.at() > FORGET_AFTER_MS);
        lastSwing.values().removeIf(e -> now - e.at() > FORGET_AFTER_MS);
    }
}

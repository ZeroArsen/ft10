package dev.zerorsen.ft10.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Turns death signals into one {@link Kill} per death.
 *
 * <p>A death message that names the killer settles a death at once. A death we only see happen (health hit 0)
 * waits a short moment for such a message and then falls back to the killer guessed from recent hits. After a
 * death is settled, further signals about the same victim are ignored for a while, so one death is never counted
 * twice.
 *
 * <p>A <em>weak</em> death is one where even the victim is a guess (a kill counter went up, so we probably killed
 * the player we last hit). It is dropped if its killer is credited with any other kill around the same time,
 * because then that kill is the one the counter meant.
 */
public final class KillDetector {
    /** How long a seen death waits for a death message before the hit-based guess is used. */
    public static final long PENDING_MS = 1_000;
    /** How long later signals about a settled victim are ignored. */
    public static final long DEDUPE_MS = 2_500;

    /** @param killer null when nobody (or nothing we know) killed the victim */
    public record Kill(PlayerRef victim, PlayerRef killer) {}

    private record Pending(PlayerRef victim, PlayerRef guessedKiller, long seenAt, boolean weak) {}

    private final Map<String, Pending> pending = new HashMap<>();
    private final Map<String, Long> settled = new HashMap<>();
    /** Killer key → when they were last credited with a kill. */
    private final Map<String, Long> lastKillBy = new HashMap<>();

    /** A death message named the victim and (maybe) the killer. */
    public Optional<Kill> onDeathMessage(PlayerRef victim, PlayerRef killer, long now) {
        if (isSettled(victim, now)) return Optional.empty();
        pending.remove(victim.key());
        return Optional.of(settle(victim, killer, now));
    }

    /** We saw the victim die but don't know who killed them yet. */
    public void onDeathSeen(PlayerRef victim, PlayerRef guessedKiller, long now) {
        onDeathSeen(victim, guessedKiller, now, false);
    }

    /** @param weak the victim is a guess too; see the class comment */
    public void onDeathSeen(PlayerRef victim, PlayerRef guessedKiller, long now, boolean weak) {
        if (isSettled(victim, now) || pending.containsKey(victim.key())) return;
        pending.put(victim.key(), new Pending(victim, guessedKiller, now, weak));
    }

    /** Settles the deaths that waited long enough for a death message. */
    public List<Kill> tick(long now) {
        List<Kill> kills = new ArrayList<>();
        // Strong deaths first, so a weak guess sees every kill they credit.
        List<Pending> due = new ArrayList<>();
        Iterator<Pending> it = pending.values().iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (now - p.seenAt() < PENDING_MS) continue;
            it.remove();
            due.add(p);
        }
        due.sort((a, b) -> Boolean.compare(a.weak(), b.weak()));
        for (Pending p : due) {
            if (p.weak() && creditedSince(p.guessedKiller(), p.seenAt() - DEDUPE_MS)) continue;
            kills.add(settle(p.victim(), p.guessedKiller(), now));
        }
        settled.values().removeIf(at -> now - at > DEDUPE_MS);
        lastKillBy.values().removeIf(at -> now - at > 2 * DEDUPE_MS);
        return kills;
    }

    public void clear() {
        pending.clear();
        settled.clear();
        lastKillBy.clear();
    }

    private Kill settle(PlayerRef victim, PlayerRef killer, long now) {
        settled.put(victim.key(), now);
        if (killer != null) lastKillBy.put(killer.key(), now);
        return new Kill(victim, killer);
    }

    private boolean creditedSince(PlayerRef killer, long since) {
        if (killer == null) return false;
        Long at = lastKillBy.get(killer.key());
        return at != null && at >= since;
    }

    private boolean isSettled(PlayerRef victim, long now) {
        Long at = settled.get(victim.key());
        return at != null && now - at <= DEDUPE_MS;
    }
}

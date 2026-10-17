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
 */
public final class KillDetector {
    /** How long a seen death waits for a death message before the hit-based guess is used. */
    public static final long PENDING_MS = 1_000;
    /** How long later signals about a settled victim are ignored. */
    public static final long DEDUPE_MS = 2_500;

    /** @param killer null when nobody (or nothing we know) killed the victim */
    public record Kill(PlayerRef victim, PlayerRef killer) {}

    private record Pending(PlayerRef victim, PlayerRef guessedKiller, long seenAt) {}

    private final Map<String, Pending> pending = new HashMap<>();
    private final Map<String, Long> settled = new HashMap<>();

    /** A death message named the victim and (maybe) the killer. */
    public Optional<Kill> onDeathMessage(PlayerRef victim, PlayerRef killer, long now) {
        if (isSettled(victim, now)) return Optional.empty();
        pending.remove(victim.key());
        settled.put(victim.key(), now);
        return Optional.of(new Kill(victim, killer));
    }

    /** We saw the victim die but don't know who killed them yet. */
    public void onDeathSeen(PlayerRef victim, PlayerRef guessedKiller, long now) {
        if (isSettled(victim, now) || pending.containsKey(victim.key())) return;
        pending.put(victim.key(), new Pending(victim, guessedKiller, now));
    }

    /** Settles the deaths that waited long enough for a death message. */
    public List<Kill> tick(long now) {
        List<Kill> kills = new ArrayList<>();
        Iterator<Pending> it = pending.values().iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (now - p.seenAt() < PENDING_MS) continue;
            it.remove();
            settled.put(p.victim().key(), now);
            kills.add(new Kill(p.victim(), p.guessedKiller()));
        }
        settled.values().removeIf(at -> now - at > DEDUPE_MS);
        return kills;
    }

    public void clear() {
        pending.clear();
        settled.clear();
    }

    private boolean isSettled(PlayerRef victim, long now) {
        Long at = settled.get(victim.key());
        return at != null && now - at <= DEDUPE_MS;
    }
}

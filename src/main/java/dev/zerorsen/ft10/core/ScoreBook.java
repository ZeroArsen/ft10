package dev.zerorsen.ft10.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Every rivalry on one server, plus which one is on screen and an undo history. */
public final class ScoreBook {
    private static final int HISTORY_LIMIT = 64;

    private final Map<String, Rivalry> rivals = new LinkedHashMap<>();
    private final Deque<Rivalry.Snapshot> history = new ArrayDeque<>();
    private String activeKey;
    private boolean dirty;

    /** Gets or creates the rivalry with this player and refreshes their name. */
    public Rivalry rivalry(PlayerRef player) {
        Rivalry rivalry = rivals.computeIfAbsent(player.key(), key -> new Rivalry(key, player.name()));
        rivalry.rename(player.name());
        return rivalry;
    }

    public Optional<Rivalry> find(String key) {
        return Optional.ofNullable(rivals.get(key));
    }

    public Optional<Rivalry> findByName(String name) {
        return rivals.values().stream().filter(r -> r.name().equalsIgnoreCase(name)).findFirst();
    }

    /** Rivalries, most recently fought first. */
    public List<Rivalry> byLastFought() {
        List<Rivalry> list = new ArrayList<>(rivals.values());
        list.sort(Comparator.comparingLong(Rivalry::lastFought).reversed());
        return list;
    }

    public Collection<Rivalry> all() {
        return Collections.unmodifiableCollection(rivals.values());
    }

    /** Adds a rivalry loaded from disk. */
    public void put(Rivalry rivalry) {
        rivals.put(rivalry.key(), rivalry);
    }

    public Optional<Rivalry> active() {
        return activeKey == null ? Optional.empty() : find(activeKey);
    }

    /** Puts this player's score on screen. Returns true when the active opponent changed. */
    public boolean setActive(PlayerRef player) {
        Rivalry rivalry = rivalry(player);
        if (rivalry.key().equals(activeKey)) return false;
        activeKey = rivalry.key();
        return true;
    }

    public void clearActive() {
        activeKey = null;
    }

    /**
     * Gives a point to {@code scorer} in the rivalry with {@code opponent}.
     *
     * @param goal       first-to target, 0 for none
     * @param autoNewSet start the next set at 0 - 0 once the goal is reached
     */
    public ScoreUpdate award(PlayerRef opponent, Side scorer, int goal, boolean autoNewSet, long now) {
        Rivalry r = rivalry(opponent);
        remember(r);

        if (scorer == Side.ME) r.setScore(r.me() + 1, r.them());
        else r.setScore(r.me(), r.them() + 1);
        r.touch(now);

        ScoreUpdate.SetResult set = null;
        int scorerScore = scorer == Side.ME ? r.me() : r.them();
        // Only the point that lands exactly on the goal ends a set, so a set that keeps going (autoNewSet off)
        // doesn't announce a win on every later point.
        if (goal > 0 && scorerScore == goal) {
            int finalMe = r.me();
            int finalThem = r.them();
            if (scorer == Side.ME) r.setSets(r.setsMe() + 1, r.setsThem());
            else r.setSets(r.setsMe(), r.setsThem() + 1);
            if (autoNewSet) r.setScore(0, 0);
            set = new ScoreUpdate.SetResult(scorer, goal, finalMe, finalThem, r.setsMe(), r.setsThem());
        }

        dirty = true;
        return new ScoreUpdate(r, scorer, r.me(), r.them(), set);
    }

    /** Sets the score by hand (undoable). */
    public Rivalry set(PlayerRef opponent, int me, int them, long now) {
        Rivalry r = rivalry(opponent);
        remember(r);
        r.setScore(me, them);
        r.touch(now);
        dirty = true;
        return r;
    }

    /** Reverts the last point or manual change. Returns the rivalry that changed. */
    public Optional<Rivalry> undo() {
        while (!history.isEmpty()) {
            Rivalry.Snapshot snapshot = history.pop();
            Rivalry r = rivals.get(snapshot.key());
            if (r == null) continue; // reset since then
            r.restore(snapshot);
            dirty = true;
            return Optional.of(r);
        }
        return Optional.empty();
    }

    public boolean canUndo() {
        return history.stream().anyMatch(s -> rivals.containsKey(s.key()));
    }

    public boolean reset(String key) {
        Rivalry removed = rivals.remove(key);
        if (removed == null) return false;
        if (key.equals(activeKey)) activeKey = null;
        dirty = true;
        return true;
    }

    public int resetAll() {
        int count = rivals.size();
        rivals.clear();
        history.clear();
        activeKey = null;
        dirty = true;
        return count;
    }

    public boolean isEmpty() {
        return rivals.isEmpty();
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markClean() {
        dirty = false;
    }

    private void remember(Rivalry r) {
        history.push(r.snapshot());
        while (history.size() > HISTORY_LIMIT) history.removeLast();
    }
}

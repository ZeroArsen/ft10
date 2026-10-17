package dev.zerorsen.ft10.core;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The scoring brain for one connection to one server. The Minecraft side feeds it hits and deaths; it decides
 * who is on screen and who gets a point. It has no Minecraft imports so every rule can be unit tested.
 */
public final class Session {
    /**
     * @param combatWindowMs how long after a hit that hit still earns the kill
     * @param switchLockMs   while we traded hits with the shown opponent this recently, others can't take the screen
     * @param switchOnHit    show whoever we start fighting (otherwise only kills switch the screen)
     * @param goal           first-to target, 0 for none
     * @param autoNewSet     start a new set at 0 - 0 once someone reaches the goal
     */
    public record Rules(long combatWindowMs, long switchLockMs, boolean switchOnHit, int goal, boolean autoNewSet) {}

    public interface Listener {
        void onScore(ScoreUpdate update);

        void onActiveChanged(Rivalry rivalry);
    }

    private final PlayerRef self;
    private final ScoreBook book;
    private final Supplier<Rules> rules;
    private final Listener listener;
    private final CombatTracker tracker;
    private final KillDetector detector = new KillDetector();
    private PlayerRef locked;
    private long lastActivity;

    public Session(PlayerRef self, ScoreBook book, Supplier<Rules> rules, Listener listener) {
        this.self = self;
        this.book = book;
        this.rules = rules;
        this.listener = listener;
        this.tracker = new CombatTracker(self);
    }

    public PlayerRef self() { return self; }
    public ScoreBook book() { return book; }
    public Optional<PlayerRef> locked() { return Optional.ofNullable(locked); }

    /** Last time we fought someone or a point was scored; drives the "only in combat" HUD mode. */
    public long lastActivity() { return lastActivity; }

    /** {@code attacker} damaged {@code victim} (any two players we can see). */
    public void onHit(PlayerRef attacker, PlayerRef victim, long now) {
        if (attacker == null || victim == null || attacker.sameAs(victim)) return;
        tracker.recordHit(attacker, victim, now);
        if (self.sameAs(attacker)) engage(victim, now);
        else if (self.sameAs(victim)) engage(attacker, now);
    }

    /** We swung at another player. Starts a fight even if the server didn't register damage (shield, i-frames). */
    public void onSwing(PlayerRef target, long now) {
        if (target == null || self.sameAs(target)) return;
        tracker.recordSwing(target, now);
        engage(target, now);
    }

    /** A death message named {@code victim} and, if the message has one, {@code killer}. */
    public void onDeathMessage(PlayerRef victim, PlayerRef killer, long now) {
        detector.onDeathMessage(victim, killer, now).ifPresent(kill -> settle(kill, now));
    }

    /** We saw {@code victim} die (or got our own death screen) without knowing the killer. */
    public void onDeathSeen(PlayerRef victim, long now) {
        PlayerRef guess = tracker.lastAttacker(victim, now, rules.get().combatWindowMs()).orElse(null);
        detector.onDeathSeen(victim, guess, now);
    }

    public void tick(long now) {
        for (KillDetector.Kill kill : detector.tick(now)) settle(kill, now);
        tracker.prune(now);
    }

    /** Pins the screen to one opponent until {@link #unlock()}. */
    public void lock(PlayerRef opponent) {
        locked = opponent;
        if (book.setActive(opponent)) listener.onActiveChanged(book.rivalry(opponent));
    }

    public void unlock() {
        locked = null;
    }

    /** Puts an opponent on screen by hand (e.g. after /ft set). */
    public void show(PlayerRef opponent) {
        if (locked != null && !locked.sameAs(opponent)) return;
        if (book.setActive(opponent)) listener.onActiveChanged(book.rivalry(opponent));
    }

    private void engage(PlayerRef other, long now) {
        lastActivity = now;
        Rules r = rules.get();
        if (!r.switchOnHit() || locked != null) return;
        Optional<Rivalry> active = book.active();
        if (active.isPresent() && !active.get().key().equals(other.key())
            && tracker.recentlyFought(active.get().key(), now, r.switchLockMs())) {
            return; // still fighting the one on screen; a stray hit from someone else doesn't steal it
        }
        show(other);
    }

    private void settle(KillDetector.Kill kill, long now) {
        PlayerRef victim = kill.victim();
        PlayerRef killer = kill.killer();
        tracker.forget(victim);

        if (self.sameAs(victim)) {
            tracker.forgetExchanges();
            if (killer != null && !self.sameAs(killer)) point(killer, Side.THEM, now);
        } else if (killer != null && self.sameAs(killer)) {
            point(victim, Side.ME, now);
        }
    }

    private void point(PlayerRef opponent, Side scorer, long now) {
        Rules r = rules.get();
        ScoreUpdate update = book.award(opponent, scorer, r.goal(), r.autoNewSet(), now);
        lastActivity = now;
        show(opponent);
        listener.onScore(update);
    }
}

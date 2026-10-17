package dev.zerorsen.ft10.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SessionTest {
    private static final PlayerRef ME = PlayerRef.of(UUID.randomUUID(), "Me");
    private static final PlayerRef ALEX = PlayerRef.of(UUID.randomUUID(), "Alex");
    private static final PlayerRef BOB = PlayerRef.of(UUID.randomUUID(), "Bob");
    private static final PlayerRef CARL = PlayerRef.of(UUID.randomUUID(), "Carl");

    private Session.Rules rules;
    private ScoreBook book;
    private Session session;
    private final List<ScoreUpdate> updates = new ArrayList<>();
    private long now;

    @BeforeEach
    void setUp() {
        rules = new Session.Rules(10_000, 5_000, true, 0, true);
        book = new ScoreBook();
        updates.clear();
        now = 1_000_000;
        session = new Session(ME, book, () -> rules, new Session.Listener() {
            @Override public void onScore(ScoreUpdate update) { updates.add(update); }
            @Override public void onActiveChanged(Rivalry rivalry) {}
        });
    }

    @Test
    void killByDeathMessageScoresForMe() {
        session.onHit(ME, ALEX, now);
        session.onDeathMessage(ALEX, ME, now + 100);

        assertScore(ALEX, 1, 0);
        assertActive(ALEX);
    }

    @Test
    void deathByDeathMessageScoresForThem() {
        session.onHit(ALEX, ME, now);
        session.onDeathMessage(ME, ALEX, now + 100);

        assertScore(ALEX, 0, 1);
    }

    @Test
    void seenDeathFallsBackToLastHitAfterWaiting() {
        session.onHit(ME, ALEX, now);
        session.onDeathSeen(ALEX, now + 200);
        session.tick(now + 300);
        assertTrue(updates.isEmpty(), "waits for a death message first");

        session.tick(now + 200 + KillDetector.PENDING_MS);
        assertScore(ALEX, 1, 0);
    }

    @Test
    void deathMessageAndSeenDeathCountOnce() {
        session.onHit(ME, ALEX, now);
        session.onDeathMessage(ALEX, ME, now + 50);
        session.onDeathSeen(ALEX, now + 100);
        session.tick(now + 5_000);

        assertEquals(1, updates.size());
        assertScore(ALEX, 1, 0);
    }

    @Test
    void deathMessageBeatsHitGuess() {
        // We hit Alex, but Bob finished him: the message is the truth.
        session.onHit(ME, ALEX, now);
        session.onDeathSeen(ALEX, now + 100);
        session.onDeathMessage(ALEX, BOB, now + 300);
        session.tick(now + 5_000);

        assertTrue(updates.isEmpty());
    }

    @Test
    void killStolenByThirdPartyIsNotOurs() {
        session.onHit(ME, ALEX, now);
        session.onHit(BOB, ALEX, now + 500);
        session.onDeathSeen(ALEX, now + 600);
        session.tick(now + 5_000);

        assertTrue(updates.isEmpty());
    }

    @Test
    void oldHitsDoNotEarnTheKill() {
        session.onHit(ME, ALEX, now);
        session.onDeathSeen(ALEX, now + 20_000);
        session.tick(now + 30_000);

        assertTrue(updates.isEmpty());
    }

    @Test
    void scoresAreKeptPerOpponentAndResumed() {
        for (int i = 0; i < 4; i++) killed(ALEX);
        diedTo(ALEX);
        assertScore(ALEX, 4, 1);

        killed(BOB);
        assertActive(BOB);
        assertScore(BOB, 1, 0);

        now += 10_000;
        session.onHit(ALEX, ME, now); // fighting Alex again
        assertActive(ALEX);
        killed(ALEX);
        assertScore(ALEX, 5, 1);
    }

    @Test
    void firstHitSwitchesTheScreen() {
        killed(ALEX);
        now += 10_000;
        session.onSwing(BOB, now);

        assertActive(BOB);
    }

    @Test
    void strayHitDoesNotStealTheScreenMidFight() {
        session.onHit(ME, ALEX, now);
        session.onHit(ALEX, ME, now + 500);
        session.onHit(CARL, ME, now + 1_000);

        assertActive(ALEX);

        session.onHit(CARL, ME, now + 7_000); // Alex fight went quiet for > 5 s
        assertActive(CARL);
    }

    @Test
    void finishedFightReleasesTheScreenImmediately() {
        session.onHit(ME, ALEX, now);
        session.onDeathMessage(ALEX, ME, now + 100);
        session.onHit(BOB, ME, now + 200);

        assertActive(BOB);
    }

    @Test
    void lockKeepsTheScreenButStillCountsOthers() {
        session.lock(ALEX);
        session.onHit(ME, BOB, now);
        session.onDeathMessage(BOB, ME, now + 100);

        assertActive(ALEX);
        assertScore(BOB, 1, 0);
    }

    @Test
    void killsBetweenOtherPlayersAreIgnored() {
        session.onHit(ALEX, BOB, now);
        session.onDeathMessage(BOB, ALEX, now + 100);
        session.onDeathSeen(CARL, now + 100);
        session.tick(now + 5_000);

        assertTrue(updates.isEmpty());
    }

    @Test
    void dyingWithoutAKillerScoresNothing() {
        session.onDeathMessage(ME, null, now);
        assertTrue(updates.isEmpty());
    }

    @Test
    void goalFinishesSetAndStartsANewOne() {
        rules = new Session.Rules(10_000, 5_000, true, 3, true);
        killed(ALEX);
        diedTo(ALEX);
        killed(ALEX);
        killed(ALEX);

        ScoreUpdate last = updates.get(updates.size() - 1);
        assertNotNull(last.set());
        assertEquals(Side.ME, last.set().winner());
        assertEquals(3, last.set().finalMe());
        assertEquals(1, last.set().finalThem());
        assertScore(ALEX, 0, 0);
        assertEquals(1, book.rivalry(ALEX).setsMe());
    }

    @Test
    void undoRevertsAFinishedSet() {
        rules = new Session.Rules(10_000, 5_000, true, 2, true);
        killed(ALEX);
        killed(ALEX);
        assertEquals(1, book.rivalry(ALEX).setsMe());

        book.undo();
        assertScore(ALEX, 1, 0);
        assertEquals(0, book.rivalry(ALEX).setsMe());
    }

    @Test
    void withoutAutoNewSetTheGoalIsAnnouncedOnce() {
        rules = new Session.Rules(10_000, 5_000, true, 2, false);
        killed(ALEX);
        killed(ALEX);
        killed(ALEX);

        assertNotNull(updates.get(1).set());
        assertNull(updates.get(2).set());
        assertScore(ALEX, 3, 0);
    }

    @Test
    void nameOnlyRefMatchesByName() {
        PlayerRef byName = PlayerRef.named("me");
        assertTrue(ME.sameAs(byName));
    }

    private void killed(PlayerRef victim) {
        now += 1_000;
        session.onHit(ME, victim, now);
        session.onDeathMessage(victim, ME, now + 50);
        now += 3_000;
        session.tick(now);
    }

    private void diedTo(PlayerRef killer) {
        now += 1_000;
        session.onHit(killer, ME, now);
        session.onDeathMessage(ME, killer, now + 50);
        now += 3_000;
        session.tick(now);
    }

    private void assertScore(PlayerRef opponent, int me, int them) {
        Rivalry r = book.rivalry(opponent);
        assertEquals(me + " - " + them, r.me() + " - " + r.them(), "score vs " + opponent.name());
    }

    private void assertActive(PlayerRef opponent) {
        assertEquals(opponent.name(), book.active().map(Rivalry::name).orElse(null));
    }
}

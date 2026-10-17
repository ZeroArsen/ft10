package dev.zerorsen.ft10.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RepeatGuardTest {
    @Test
    void repeatedTextPassesOnceWhileItKeepsComing() {
        RepeatGuard guard = new RepeatGuard(8_000);
        assertTrue(guard.isNew("You killed Steve", 0));
        // An action bar re-sent every tick for 20 s is still one message.
        for (long t = 50; t <= 20_000; t += 50) assertFalse(guard.isNew("You killed Steve", t));
        assertTrue(guard.isNew("You killed Alex", 20_000));
    }

    @Test
    void sameTextCountsAgainAfterAPause() {
        RepeatGuard guard = new RepeatGuard(8_000);
        assertTrue(guard.isNew("You killed Steve", 0));
        assertTrue(guard.isNew("You killed Steve", 8_001));
    }
}

package dev.zerorsen.ft10.core;

import static dev.zerorsen.ft10.core.DeathMessageParser.SELF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeathMessageParserTest {
    private final DeathMessageParser presets = new DeathMessageParser(true, List.of());

    @Test
    void readsCommonFormats() {
        assertParsed("Steve was slain by Alex", "Steve", "Alex");
        assertParsed("☠ Steve was killed by Alex (4.5❤)", "Steve", "Alex");
        assertParsed("[VIP] Steve has been eliminated by Alex_99", "Steve", "Alex_99");
        assertParsed("⚔ Alex killed Steve", "Steve", "Alex");
        assertParsed("» Alex has slain Steve with Diamond Sword", "Steve", "Alex");
        assertParsed("Steve died to Alex", "Steve", "Alex");
        assertParsed("§cSteve §7was slain by §aAlex", "Steve", "Alex");
        assertParsed(".BedrockGuy was slain by Alex", ".BedrockGuy", "Alex");
        assertParsed("STEVE WAS KILLED BY ALEX!", "STEVE", "ALEX");
    }

    @Test
    void readsSecondPersonMessages() {
        assertParsed("You killed Steve!", "Steve", SELF);
        assertParsed("» You have eliminated Steve (+10 coins)", "Steve", SELF);
        assertParsed("You were slain by Alex", SELF, "Alex");
        assertParsed("You have been killed by Alex.", SELF, "Alex");
        assertParsed("Alex killed you", SELF, "Alex");
        assertParsed("You died to Alex", SELF, "Alex");
        assertParsed("KILLED Steve", "Steve", SELF);
        assertParsed("Killed by Alex", SELF, "Alex");
    }

    @Test
    void broadPatternsAreMarkedLoose() {
        DeathMessageParser.Parsed thrown = assertParsed("Steve was thrown into the void by Alex", "Steve", "Alex");
        assertTrue(thrown.loose());
        assertTrue(assertParsed("You were struck down by Alex", SELF, "Alex").loose());
        assertFalse(assertParsed("Steve was slain by Alex", "Steve", "Alex").loose());
    }

    @Test
    void ignoresPlayerChatAndModeration() {
        assertTrue(presets.parse("Alex: Bob killed Steve lol").isEmpty());
        assertTrue(presets.parse("<Alex> Steve was slain by Bob").isEmpty());
        assertTrue(presets.parse("Alex: Bob was killed by Steve").isEmpty());
        assertTrue(presets.parse("Welcome to the server!").isEmpty());
        assertTrue(presets.parse("Steve was kicked by Admin").isEmpty());
        assertTrue(presets.parse("You were invited by Alex to a party").isEmpty());
        assertTrue(presets.parse("Killed by").isEmpty());
    }

    @Test
    void templatesNeedNoRegex() {
        // Leading symbols and small-caps tags ("ꜰꜰᴀ | ") may come before a template, like before a preset.
        DeathMessageParser parser = new DeathMessageParser(false, List.of("{killer} » {victim} (+*)"));
        assertParsedBy(parser, "ꜰꜰᴀ | Alex » Bob (+1)", "Bob", "Alex");
        parser = new DeathMessageParser(false, List.of("ꜰꜰᴀ | {killer} » {victim} (+*)"));
        assertParsedBy(parser, "ꜰꜰᴀ | Alex » Bob (+1)", "Bob", "Alex");
        assertParsedBy(parser, "ꜰꜰᴀ | alex » bob (+12)", "bob", "alex");
    }

    @Test
    void templateWithOneSide() {
        DeathMessageParser killedMe = new DeathMessageParser(false, List.of("Slain by {killer}."));
        assertParsedBy(killedMe, "Slain by Alex.", SELF, "Alex");

        DeathMessageParser killerOpen = new DeathMessageParser(false, List.of("{victim} fell into the void"));
        assertParsedBy(killerOpen, "Steve fell into the void", "Steve", null);
    }

    @Test
    void templateSpecialCharactersAreLiteral() {
        DeathMessageParser parser = new DeathMessageParser(false, List.of("[FFA] {victim} (x) was hit by {killer}?"));
        assertParsedBy(parser, "[FFA] Steve (x) was hit by Alex?", "Steve", "Alex");
        assertTrue(parser.parse("[FFA] Steve x was hit by Alex").isEmpty());
    }

    @Test
    void customRegexStillWorks() {
        DeathMessageParser parser = new DeathMessageParser(false, List.of("^(?<victim>\\w+) fell into the void$"));
        Optional<DeathMessageParser.Parsed> parsed = parser.parse("Steve fell into the void");
        assertTrue(parsed.isPresent());
        assertEquals("Steve", parsed.get().victim());
        assertNull(parsed.get().killer());
        assertEquals("custom #1", parsed.get().label());
    }

    @Test
    void rejectsBadPatterns() {
        DeathMessageParser parser = new DeathMessageParser(false, List.of("(\\w+) killed", "([unclosed", "{nobody} died"));
        assertEquals(3, parser.errors().size());
        assertNull(DeathMessageParser.validate("(?<killer>\\w+) killed you"));
        assertNull(DeathMessageParser.validate("(?<victim>\\w+) died"));
        assertNull(DeathMessageParser.validate("{victim} was slain by {killer}"));
        assertNotNull(DeathMessageParser.validate("nobody here"));
    }

    @Test
    void templateBecomesAnchoredRegex() {
        String regex = DeathMessageParser.templateToRegex("{victim} died *");
        assertTrue(regex.startsWith("^"));
        assertTrue(DeathMessageParser.isTemplate("{victim} died *"));
        assertFalse(DeathMessageParser.isTemplate("(?<victim>\\w+) {victim}"));
    }

    private DeathMessageParser.Parsed assertParsed(String message, String victim, String killer) {
        return assertParsedBy(presets, message, victim, killer);
    }

    /** {@code victim == null} means "no match expected". */
    private static DeathMessageParser.Parsed assertParsedBy(DeathMessageParser parser, String message, String victim,
                                                            String killer) {
        DeathMessageParser.Parsed parsed = parser.parse(message).orElse(null);
        if (victim == null) {
            assertNull(parsed, "unexpected match: " + message);
            return null;
        }
        assertNotNull(parsed, "no match: " + message);
        assertEquals(victim, parsed.victim(), message);
        assertEquals(killer, parsed.killer(), message);
        return parsed;
    }
}

package dev.zerorsen.ft10.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    }

    @Test
    void ignoresPlayerChat() {
        assertTrue(presets.parse("Alex: Bob killed Steve lol").isEmpty());
        assertTrue(presets.parse("<Alex> Steve was slain by Bob").isEmpty());
        assertTrue(presets.parse("Welcome to the server!").isEmpty());
    }

    @Test
    void customPatternWithoutKiller() {
        DeathMessageParser parser = new DeathMessageParser(false, List.of("^(?<victim>\\w+) fell into the void$"));
        Optional<DeathMessageParser.Parsed> parsed = parser.parse("Steve fell into the void");
        assertTrue(parsed.isPresent());
        assertEquals("Steve", parsed.get().victim());
        assertNull(parsed.get().killer());
    }

    @Test
    void customPatternMatchingServerFormat() {
        DeathMessageParser parser = new DeathMessageParser(false,
            List.of("^ꜰꜰᴀ \\| (?<killer>\\w+) » (?<victim>\\w+)"));
        assertEquals("Bob", parser.parse("ꜰꜰᴀ | Alex » Bob (+1)").orElseThrow().victim());
    }

    @Test
    void rejectsBadPatterns() {
        DeathMessageParser parser = new DeathMessageParser(false, List.of("(?<killer>\\w+) killed", "([unclosed"));
        assertEquals(2, parser.errors().size());
        assertNotNull(DeathMessageParser.validate("(?<killer>\\w+)"));
        assertNull(DeathMessageParser.validate("(?<victim>\\w+) died"));
    }

    private void assertParsed(String message, String victim, String killer) {
        DeathMessageParser.Parsed parsed = presets.parse(message).orElse(null);
        assertNotNull(parsed, "no match: " + message);
        assertEquals(victim, parsed.victim(), message);
        assertEquals(killer, parsed.killer(), message);
    }
}

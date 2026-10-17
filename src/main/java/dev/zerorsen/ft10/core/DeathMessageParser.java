package dev.zerorsen.ft10.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Reads custom (non-vanilla) death messages with regular expressions. Each pattern needs a {@code victim}
 * group and may have a {@code killer} group. Vanilla translatable death messages are handled elsewhere; this is
 * for servers whose plugins send their own plain-text kill messages.
 */
public final class DeathMessageParser {
    /** A Minecraft name, optionally with the "." or "*" prefix Bedrock players get through Geyser. */
    private static final String NAME = "[.*]?\\w{1,16}";
    /**
     * Start of the message: optional symbols (☠, ⚔, », …) and an optional [RANK] tag. Anchoring here keeps a
     * player's chat line like "Alex: Bob killed Steve" from matching.
     */
    private static final String START = "^[^\\w.*\\[]*(?:\\[[^\\]]{1,24}\\]\\s*[^\\w.*]*)?";

    /** Common FFA plugin formats, used before the player's own patterns. */
    public static final List<String> PRESETS = List.of(
        START + "(?<victim>" + NAME + ") (?:was|has been|got) (?:slain|killed|murdered|destroyed|eliminated"
            + "|executed|shot|finished off|knocked out|obliterated|defeated|blown up) by (?<killer>" + NAME + ")\\b",
        START + "(?<killer>" + NAME + ") (?:has )?(?:killed|slain|slew|eliminated|destroyed|murdered|finished off"
            + "|obliterated|defeated|executed) (?<victim>" + NAME + ")\\b",
        START + "(?<victim>" + NAME + ") (?:died to|lost to|fell to) (?<killer>" + NAME + ")\\b"
    );

    /** @param killer null when the pattern has no killer group or it didn't match */
    public record Parsed(String victim, String killer) {}

    private final List<Pattern> patterns = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public DeathMessageParser(boolean usePresets, List<String> custom) {
        if (usePresets) PRESETS.forEach(this::add);
        custom.forEach(this::add);
    }

    /** Patterns that failed to compile or lack a {@code victim} group, with the reason. */
    public List<String> errors() {
        return List.copyOf(errors);
    }

    public Optional<Parsed> parse(String message) {
        String plain = strip(message);
        for (Pattern pattern : patterns) {
            Matcher m = pattern.matcher(plain);
            if (!m.find()) continue;
            String victim = m.group("victim");
            String killer = hasGroup(pattern, "killer") ? m.group("killer") : null;
            if (victim == null) continue;
            if (killer != null && killer.equalsIgnoreCase(victim)) killer = null;
            return Optional.of(new Parsed(victim, killer));
        }
        return Optional.empty();
    }

    /** Removes legacy § formatting codes and surrounding whitespace. */
    public static String strip(String message) {
        return message.replaceAll("§.", "").strip();
    }

    /** Null when {@code regex} is usable, otherwise why not. */
    public static String validate(String regex) {
        try {
            Pattern pattern = Pattern.compile(regex);
            return hasGroup(pattern, "victim") ? null : "missing (?<victim>...) group";
        } catch (PatternSyntaxException e) {
            return e.getDescription();
        }
    }

    private void add(String regex) {
        String error = validate(regex);
        if (error != null) {
            errors.add(regex + " -> " + error);
            return;
        }
        patterns.add(Pattern.compile(regex));
    }

    private static boolean hasGroup(Pattern pattern, String name) {
        return pattern.namedGroups().containsKey(name);
    }
}

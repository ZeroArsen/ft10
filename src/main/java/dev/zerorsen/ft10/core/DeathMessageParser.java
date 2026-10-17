package dev.zerorsen.ft10.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Reads kill messages that aren't vanilla translatable death messages: plugin chat lines, action bars and titles.
 *
 * <p>A pattern names the players with a {@code victim} and/or {@code killer} group. A pattern with only a
 * {@code killer} group is about the player themself dying ("You were slain by {killer}"); one with only a
 * {@code victim} group leaves the killer open, and the hit history decides. Players can write regexes or simple
 * templates like {@code {victim} was slain by {killer}} ({@code *} matches anything).
 */
public final class DeathMessageParser {
    /** Stands for the player themself in {@link Parsed}; never a valid Minecraft name. */
    public static final String SELF = "<self>";

    /** A Minecraft name, optionally with the "." or "*" prefix Bedrock players get through Geyser. */
    private static final String NAME = "[.*]?\\w{1,16}";
    /**
     * Start of the message: optional symbols (☠, ⚔, », …) and an optional [RANK] tag. Anchoring here keeps a
     * player's chat line like "Alex: Bob killed Steve" from matching.
     */
    private static final String START = "^[^\\w.*\\[]*(?:\\[[^\\]]{1,24}\\]\\s*[^\\w.*]*)?";
    private static final String VICTIM = "(?<victim>" + NAME + ")";
    private static final String KILLER = "(?<killer>" + NAME + ")";
    private static final String KILL_VERB = "(?:killed|slain|slew|eliminated|destroyed|murdered|finished off"
        + "|obliterated|defeated|executed|wrecked|annihilated|knocked out|took out)";
    private static final String PASSIVE_VERB = "(?:slain|killed|murdered|destroyed|eliminated|executed|shot"
        + "|finished off|knocked out|obliterated|defeated|blown up|wrecked|annihilated|taken out)";
    /**
     * One to five words, for "was struck down by", "was thrown into the void by" and similar. Moderation and
     * party messages ("was kicked by", "was invited by") are not kills.
     */
    private static final String ANY_WORDS = "(?!(?:banned|kicked|muted|warned|invited|teleported|healed|frozen|unfrozen"
        + "|promoted|demoted|challenged|added|removed|summoned|revived|reported|jailed|pardoned|unbanned|unmuted"
        + "|sent|given|paid|accepted|denied|requested|messaged|whispered|mentioned|followed|tagged|spectated)\\b)"
        + "(?:[\\p{L}'-]+ ){0,4}[\\p{L}'-]+";
    private static final String YOU = "(?:you|u)";

    private enum Self { NONE, VICTIM, KILLER }

    /** @param loose matches broadly; only trusted when the other player is someone we were just fighting */
    private record Entry(String label, Pattern pattern, Self self, boolean loose) {}

    /** Built-in formats, tried before the player's own patterns. Second-person ones come first. */
    private static final List<Entry> PRESETS = List.of(
        preset("you-killed", START + YOU + " (?:have |just )?" + KILL_VERB + " " + VICTIM + "\\b", Self.KILLER),
        preset("killed-you", START + KILLER + " (?:has |just )?" + KILL_VERB + " " + YOU + "\\b", Self.VICTIM),
        preset("you-were-killed", START + YOU + " (?:were|have been|got) " + ANY_WORDS + " by " + KILLER + "\\b",
            Self.VICTIM, true),
        preset("you-died-to", START + YOU + " (?:died to|lost to|fell to|died by) " + KILLER + "\\b", Self.VICTIM),
        preset("killed-by-title", START + "(?:killed|slain|eliminated) by " + KILLER + "\\b", Self.VICTIM),
        preset("kill-title", START + "(?:killed|eliminated)\\s*[:!»-]?\\s+(?!by\\b)" + VICTIM + "\\b",
            Self.KILLER),
        preset("was-killed-by", START + VICTIM + " (?:was|has been|got) " + PASSIVE_VERB + " by " + KILLER + "\\b",
            Self.NONE),
        preset("killed", START + KILLER + " (?:has |just )?" + KILL_VERB + " " + VICTIM + "\\b", Self.NONE),
        preset("died-to", START + VICTIM + " (?:died to|lost to|fell to) " + KILLER + "\\b", Self.NONE),
        preset("was-by", START + VICTIM + " (?:was|has been|got) " + ANY_WORDS + " by " + KILLER + "\\b",
            Self.NONE, true)
    );

    /**
     * @param victim a name, or {@link #SELF}
     * @param killer a name, {@link #SELF}, or null when the message names no killer
     * @param label  which pattern matched (a preset name, or "custom #n"), for debug output
     * @param loose  a broad pattern: count it only when the other player is a current opponent
     */
    public record Parsed(String victim, String killer, String label, boolean loose) {}

    private final List<Entry> patterns = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public DeathMessageParser(boolean usePresets, List<String> custom) {
        if (usePresets) patterns.addAll(PRESETS);
        for (int i = 0; i < custom.size(); i++) add("custom #" + (i + 1), custom.get(i));
    }

    /** Patterns that failed to compile or name no player, with the reason. */
    public List<String> errors() {
        return List.copyOf(errors);
    }

    public Optional<Parsed> parse(String message) {
        String plain = strip(message);
        if (plain.isEmpty()) return Optional.empty();
        for (Entry entry : patterns) {
            Matcher m = entry.pattern().matcher(plain);
            if (!m.find()) continue;
            String victim = group(m, entry.pattern(), "victim");
            String killer = group(m, entry.pattern(), "killer");
            if (entry.self() == Self.VICTIM || victim == null) victim = SELF;
            if (entry.self() == Self.KILLER) killer = SELF;
            if (victim.equals(SELF) && killer == null) continue; // matched nothing useful
            if (killer != null && killer.equalsIgnoreCase(victim)) killer = null;
            return Optional.of(new Parsed(victim, killer, entry.label(), entry.loose()));
        }
        return Optional.empty();
    }

    /** Removes legacy § formatting codes and surrounding whitespace. */
    public static String strip(String message) {
        return message.replaceAll("§.", "").strip();
    }

    /** Null when {@code pattern} (a regex or a template) is usable, otherwise why not. */
    public static String validate(String pattern) {
        try {
            Pattern compiled = compile(pattern);
            return hasGroup(compiled, "victim") || hasGroup(compiled, "killer")
                ? null
                : isTemplate(pattern) ? "needs {victim} or {killer}" : "needs a (?<victim>...) or (?<killer>...) group";
        } catch (PatternSyntaxException e) {
            return e.getDescription();
        }
    }

    /** A template uses {victim}/{killer} placeholders; anything with a named regex group is a regex. */
    static boolean isTemplate(String pattern) {
        return !pattern.contains("(?<") && (pattern.contains("{victim}") || pattern.contains("{killer}"));
    }

    /** "{victim} was slain by {killer}" → an anchored, case-insensitive regex; "*" matches anything. */
    static String templateToRegex(String template) {
        StringBuilder regex = new StringBuilder(START);
        StringBuilder literal = new StringBuilder();
        int i = 0;
        while (i < template.length()) {
            if (template.startsWith("{victim}", i) || template.startsWith("{killer}", i)) {
                flush(regex, literal);
                regex.append(template.startsWith("{victim}", i) ? VICTIM : KILLER);
                i += 8;
            } else if (template.charAt(i) == '*') {
                flush(regex, literal);
                regex.append(".*?");
                i++;
            } else {
                literal.append(template.charAt(i++));
            }
        }
        flush(regex, literal);
        return regex.toString();
    }

    private static void flush(StringBuilder regex, StringBuilder literal) {
        if (literal.isEmpty()) return;
        regex.append(Pattern.quote(literal.toString()));
        literal.setLength(0);
    }

    private static Pattern compile(String pattern) {
        if (isTemplate(pattern.strip())) {
            return Pattern.compile(templateToRegex(pattern.strip()), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        }
        return Pattern.compile(pattern);
    }

    private void add(String label, String pattern) {
        String error = validate(pattern);
        if (error != null) {
            errors.add(pattern + " -> " + error);
            return;
        }
        patterns.add(new Entry(label, compile(pattern), Self.NONE, false));
    }

    private static Entry preset(String label, String regex, Self self) {
        return preset(label, regex, self, false);
    }

    private static Entry preset(String label, String regex, Self self, boolean loose) {
        return new Entry(label, Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE), self, loose);
    }

    private static String group(Matcher m, Pattern pattern, String name) {
        return hasGroup(pattern, name) ? m.group(name) : null;
    }

    private static boolean hasGroup(Pattern pattern, String name) {
        return pattern.namedGroups().containsKey(name);
    }
}

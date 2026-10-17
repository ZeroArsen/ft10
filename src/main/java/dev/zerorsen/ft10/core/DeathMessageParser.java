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
 *
 * <p>Duel servers often only name the round's winner ("Steve won the round 1 - 0"): a {@code winner} group means
 * "the fight between the player and their current opponent ended, and this one won".
 */
public final class DeathMessageParser {
    /** Stands for the player themself in {@link Parsed}; never a valid Minecraft name. */
    public static final String SELF = "<self>";
    /** Stands for the opponent the player is fighting right now ("Round Won"). */
    public static final String OPPONENT = "<opponent>";

    /** A Minecraft name, optionally with the "." or "*" prefix Bedrock players get through Geyser. */
    private static final String NAME = "[.*]?\\w{1,16}";
    /**
     * Start of the message: optional symbols (☠, ⚔, », …) and an optional [RANK] tag. Anchoring here keeps a
     * player's chat line like "Alex: Bob killed Steve" from matching.
     */
    private static final String START = "^[^\\w.*\\[]*(?:\\[[^\\]]{1,24}\\]\\s*[^\\w.*]*)?";
    private static final String VICTIM = "(?<victim>" + NAME + ")";
    private static final String KILLER = "(?<killer>" + NAME + ")";
    private static final String WINNER = "(?<winner>" + NAME + ")";
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

    /**
     * Which side a preset leaves implicit: the player is the victim or the killer, the current opponent died to
     * the player ("Round Won"), or the player died to whoever hit them last ("You Died").
     */
    private enum Self { NONE, VICTIM, KILLER, OPPONENT_DIED, SELF_DIED }

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
            Self.NONE, true),
        // Duel servers: the round's winner, or a title for each side.
        preset("won-the-round", START + WINNER + " (?:has )?(?:won|wins) the (?:round|duel|match|fight)\\b", Self.NONE),
        preset("round-won-by", START + "(?:round|duel|match) won by " + WINNER + "\\b", Self.NONE),
        preset("round-won-title", START + "(?:round won|you won the round)\\b", Self.OPPONENT_DIED),
        preset("you-died-title", START + "you died\\b(?!\\s+(?:to|by)\\b)", Self.SELF_DIED)
    );

    /**
     * @param victim a name, {@link #SELF} or {@link #OPPONENT}; null when only the winner is known
     * @param killer a name, {@link #SELF}, or null when the message names no killer
     * @param winner the winner of a duel round, or null; then victim and killer are null
     * @param label  which pattern matched (a preset name, or "custom #n"), for debug output
     * @param loose  a broad pattern: count it only when the other player is a current opponent
     */
    public record Parsed(String victim, String killer, String winner, String label, boolean loose) {}

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
            String winner = group(m, entry.pattern(), "winner");
            if (winner != null) return Optional.of(new Parsed(null, null, winner, entry.label(), entry.loose()));
            if (entry.self() == Self.OPPONENT_DIED) {
                return Optional.of(new Parsed(OPPONENT, SELF, null, entry.label(), entry.loose()));
            }
            if (entry.self() == Self.SELF_DIED) {
                return Optional.of(new Parsed(SELF, null, null, entry.label(), entry.loose()));
            }
            String victim = group(m, entry.pattern(), "victim");
            String killer = group(m, entry.pattern(), "killer");
            if (entry.self() == Self.VICTIM || victim == null) victim = SELF;
            if (entry.self() == Self.KILLER) killer = SELF;
            if (victim.equals(SELF) && killer == null) continue; // matched nothing useful
            if (killer != null && killer.equalsIgnoreCase(victim)) killer = null;
            return Optional.of(new Parsed(victim, killer, null, entry.label(), entry.loose()));
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
            return hasGroup(compiled, "victim") || hasGroup(compiled, "killer") || hasGroup(compiled, "winner")
                ? null
                : isTemplate(pattern)
                    ? "needs {victim}, {killer} or {winner}"
                    : "needs a (?<victim>...), (?<killer>...) or (?<winner>...) group";
        } catch (PatternSyntaxException e) {
            return e.getDescription();
        }
    }

    /** A template uses {victim}/{killer}/{winner} placeholders; anything with a named regex group is a regex. */
    static boolean isTemplate(String pattern) {
        return !pattern.contains("(?<")
            && (pattern.contains("{victim}") || pattern.contains("{killer}") || pattern.contains("{winner}"));
    }

    /** "{victim} was slain by {killer}" → an anchored, case-insensitive regex; "*" matches anything. */
    static String templateToRegex(String template) {
        StringBuilder regex = new StringBuilder(START);
        StringBuilder literal = new StringBuilder();
        int i = 0;
        while (i < template.length()) {
            String placeholder = placeholderAt(template, i);
            if (placeholder != null) {
                flush(regex, literal);
                regex.append(switch (placeholder) {
                    case "{victim}" -> VICTIM;
                    case "{killer}" -> KILLER;
                    default -> WINNER;
                });
                i += placeholder.length();
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

    private static String placeholderAt(String template, int i) {
        for (String placeholder : List.of("{victim}", "{killer}", "{winner}")) {
            if (template.startsWith(placeholder, i)) return placeholder;
        }
        return null;
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

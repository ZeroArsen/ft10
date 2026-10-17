package dev.zerorsen.ft10.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Watches the "Kills: 12" / "Deaths: 3" lines many FFA servers put in the sidebar. Some servers never really
 * kill a player (they teleport and heal them), so no death reaches the client; the counter going up by one is then
 * the only sign. Only steps of exactly one count: the first reading, jumps and resets just move the baseline.
 */
public final class StatWatcher {
    public enum Stat { KILLS, DEATHS }

    /** One sidebar row: its text and the score number drawn next to it. */
    public record Line(String text, int value) {}

    public record Change(Stat stat, int from, int to) {}

    private static final String NUMBER = "(\\d[\\d,.]{0,8})";

    private Integer kills;
    private Integer deaths;

    /** Reads the sidebar ({@code lines} empty when there is none) and reports counters that went up by one. */
    public List<Change> update(List<Line> lines, List<String> killLabels, List<String> deathLabels) {
        List<Change> changes = new ArrayList<>();
        Integer newKills = find(lines, killLabels).orElse(null);
        Integer newDeaths = find(lines, deathLabels).orElse(null);
        if (stepped(kills, newKills)) changes.add(new Change(Stat.KILLS, kills, newKills));
        if (stepped(deaths, newDeaths)) changes.add(new Change(Stat.DEATHS, deaths, newDeaths));
        kills = newKills;
        deaths = newDeaths;
        return changes;
    }

    public void clear() {
        kills = null;
        deaths = null;
    }

    private static boolean stepped(Integer before, Integer after) {
        return before != null && after != null && after == before + 1;
    }

    /** The counter behind the first line that carries one of {@code labels}. */
    Optional<Integer> find(List<Line> lines, List<String> labels) {
        for (String label : labels) {
            if (label == null || label.isBlank()) continue;
            Pattern[] patterns = patterns(label.strip());
            for (Line line : lines) {
                String text = DeathMessageParser.strip(line.text());
                if (!patterns[0].matcher(text).find()) continue;
                Matcher after = patterns[1].matcher(text);
                if (after.find()) return parse(after.group(1));
                Matcher before = patterns[2].matcher(text);
                if (before.find()) return parse(before.group(1));
                return Optional.of(line.value()); // "Kills" with the number in the score column
            }
        }
        return Optional.empty();
    }

    private Pattern[] patterns(String label) {
        String word = "(?<![\\p{L}])" + Pattern.quote(label) + "(?![\\p{L}])";
        int flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        return new Pattern[] {
            Pattern.compile(word, flags),
            Pattern.compile(word + "\\s*[:»|=\\-]?\\s*" + NUMBER, flags),
            Pattern.compile(NUMBER + "\\s*" + word, flags),
        };
    }

    private static Optional<Integer> parse(String number) {
        String digits = number.replaceAll("\\D", "");
        if (digits.isEmpty() || digits.length() > 9) return Optional.empty();
        return Optional.of(Integer.parseInt(digits));
    }
}

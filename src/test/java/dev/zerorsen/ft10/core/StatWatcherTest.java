package dev.zerorsen.ft10.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class StatWatcherTest {
    private static final List<String> KILLS = List.of("Kills");
    private static final List<String> DEATHS = List.of("Deaths");

    private final StatWatcher watcher = new StatWatcher();

    @Test
    void firstReadingOnlySetsTheBaseline() {
        assertTrue(update("Kills: 12", "Deaths: 3").isEmpty());
    }

    @Test
    void stepOfOneIsReported() {
        update("Kills: 12", "Deaths: 3");
        List<StatWatcher.Change> changes = update("Kills: 13", "Deaths: 3");
        assertEquals(List.of(new StatWatcher.Change(StatWatcher.Stat.KILLS, 12, 13)), changes);
        assertEquals(List.of(new StatWatcher.Change(StatWatcher.Stat.DEATHS, 3, 4)), update("Kills: 13", "Deaths: 4"));
    }

    @Test
    void jumpsAndResetsMoveTheBaseline() {
        update("Kills: 12");
        assertTrue(update("Kills: 20").isEmpty());
        assertTrue(update("Kills: 0").isEmpty());
        assertEquals(1, update("Kills: 1").size());
    }

    @Test
    void sidebarGoneResetsTheBaseline() {
        update("Kills: 5");
        assertTrue(watcher.update(List.of(), KILLS, DEATHS).isEmpty());
        assertTrue(update("Kills: 6").isEmpty(), "first reading after the sidebar came back");
    }

    @Test
    void readsCommonLayouts() {
        assertEquals(12, find("§7Kills: §a12"));
        assertEquals(12, find(" ⚔ KILLS » 12"));
        assertEquals(1234, find("Kills: 1,234"));
        assertEquals(12, find("12 Kills"));
        assertEquals(-1, find("Kill Streak: 4"));
        assertEquals(-1, find("Skills: 4"));
    }

    @Test
    void numberInTheScoreColumn() {
        List<StatWatcher.Line> lines = List.of(new StatWatcher.Line("Kills", 7));
        assertEquals(7, watcher.find(lines, KILLS).orElse(-1));
    }

    private int find(String text) {
        return watcher.find(List.of(new StatWatcher.Line(text, 0)), KILLS).orElse(-1);
    }

    private List<StatWatcher.Change> update(String... texts) {
        List<StatWatcher.Line> lines = java.util.Arrays.stream(texts).map(t -> new StatWatcher.Line(t, 0)).toList();
        return watcher.update(lines, KILLS, DEATHS);
    }
}

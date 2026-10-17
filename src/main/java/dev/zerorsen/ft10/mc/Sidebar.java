package dev.zerorsen.ft10.mc;

import dev.zerorsen.ft10.core.StatWatcher;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/**
 * The sidebar as the player sees it: each row's text (team prefix + name + suffix, which is how servers draw
 * "Kills: 12") and its score number. Same API on 1.21.11 and 26.2.
 */
public final class Sidebar {
    private Sidebar() {}

    public static List<StatWatcher.Line> lines(ClientLevel level) {
        Scoreboard scoreboard = level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) return List.of();
        List<StatWatcher.Line> lines = new ArrayList<>();
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            if (entry.isHidden()) continue;
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            Component text = PlayerTeam.formatNameForTeam(team, entry.ownerName());
            lines.add(new StatWatcher.Line(text.getString(), entry.value()));
        }
        return lines;
    }
}

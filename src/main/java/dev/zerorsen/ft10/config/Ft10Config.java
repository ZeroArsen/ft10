package dev.zerorsen.ft10.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Every user setting, saved as {@code config/ft10.json}. Field names are the JSON keys, so renaming one drops
 * that setting for existing users.
 */
public final class Ft10Config {
    public enum HudVisibility { ALWAYS, IN_COMBAT }

    public static final int DEFAULT_COLOR_ME = 0x04E525;
    public static final int DEFAULT_COLOR_THEM = 0xE50404;
    public static final int DEFAULT_COLOR_SEPARATOR = 0x555555;
    public static final int DEFAULT_COLOR_LABEL = 0x919191;

    public boolean enabled = true;

    // HUD
    public boolean hudEnabled = true;
    public HudVisibility hudVisibility = HudVisibility.ALWAYS;
    public int hudHideAfterSeconds = 30;
    public int hudOffsetX = 0;
    public int hudOffsetY = 0;
    public float hudScale = 1.0f;
    public boolean hudShadow = true;
    public boolean hudShowName = true;
    public boolean hudShowGoal = true;
    public int colorMe = DEFAULT_COLOR_ME;
    public int colorThem = DEFAULT_COLOR_THEM;
    public int colorSeparator = DEFAULT_COLOR_SEPARATOR;
    public int colorLabel = DEFAULT_COLOR_LABEL;

    // Feedback
    public boolean titleEnabled = true;
    public int titleFadeIn = 3;
    public int titleStay = 25;
    public int titleFadeOut = 8;
    public boolean chatEnabled = true;
    public boolean soundEnabled = true;
    public float soundVolume = 1.0f;

    // Detection
    public int combatTimeoutSeconds = 10;
    public int switchLockSeconds = 5;
    public boolean switchOnHit = true;
    public boolean usePresetPatterns = true;
    public List<String> customPatterns = new ArrayList<>();
    /** Read "Kills: n" / "Deaths: n" from the sidebar, for servers that fake deaths. */
    public boolean useSidebarStats = true;
    public List<String> statKillLabels = new ArrayList<>(List.of("Kills"));
    public List<String> statDeathLabels = new ArrayList<>(List.of("Deaths"));
    /** An opponent who leaves the server mid-fight counts as our kill. */
    public boolean countCombatLogs = false;

    // First-to goal
    public int goal = 0;
    public boolean autoNewSet = true;

    // Data and commands
    public boolean saveScores = true;
    public boolean shortAlias = true;

    /** Brings hand-edited or outdated values back into range. */
    void sanitize() {
        if (hudVisibility == null) hudVisibility = HudVisibility.ALWAYS;
        if (customPatterns == null) customPatterns = new ArrayList<>();
        if (statKillLabels == null) statKillLabels = new ArrayList<>(List.of("Kills"));
        if (statDeathLabels == null) statDeathLabels = new ArrayList<>(List.of("Deaths"));
        hudHideAfterSeconds = clamp(hudHideAfterSeconds, 1, 600);
        hudScale = Math.max(0.5f, Math.min(3.0f, hudScale));
        titleFadeIn = clamp(titleFadeIn, 0, 100);
        titleStay = clamp(titleStay, 1, 200);
        titleFadeOut = clamp(titleFadeOut, 0, 100);
        soundVolume = Math.max(0f, Math.min(1f, soundVolume));
        combatTimeoutSeconds = clamp(combatTimeoutSeconds, 1, 60);
        switchLockSeconds = clamp(switchLockSeconds, 0, 60);
        goal = clamp(goal, 0, 999);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

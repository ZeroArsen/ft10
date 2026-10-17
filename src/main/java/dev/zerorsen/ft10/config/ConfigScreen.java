package dev.zerorsen.ft10.config;

import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.StringControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import dev.zerorsen.ft10.Ft10;
import dev.zerorsen.ft10.compat.ClientCompat;
import dev.zerorsen.ft10.compat.HudCompat;
import dev.zerorsen.ft10.core.DeathMessageParser;
import dev.zerorsen.ft10.feedback.HudEditorScreen;
import dev.zerorsen.ft10.feedback.Texts;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The settings screen (YACL), opened from Mod Menu or {@code /ft config}. */
public final class ConfigScreen {
    private ConfigScreen() {}

    public static Screen create(Screen parent) {
        Ft10Config c = Ft10.get().config();
        Ft10Config d = new Ft10Config();

        return YetAnotherConfigLib.createBuilder()
            .title(Component.translatable("ft10.config.title"))
            .category(ConfigCategory.createBuilder()
                .name(text("category.general"))
                .option(bool("enabled", d.enabled, () -> c.enabled, v -> c.enabled = v))
                .option(Option.<Integer>createBuilder()
                    .name(text("goal"))
                    .description(describe("goal"))
                    .binding(d.goal, () -> c.goal, v -> c.goal = v)
                    .controller(o -> IntegerFieldControllerBuilder.create(o).range(0, 999))
                    .build())
                .option(bool("autoNewSet", d.autoNewSet, () -> c.autoNewSet, v -> c.autoNewSet = v))
                .option(bool("saveScores", d.saveScores, () -> c.saveScores, v -> c.saveScores = v))
                .option(bool("shortAlias", d.shortAlias, () -> c.shortAlias, v -> c.shortAlias = v))
                .option(resetButton("resetServer", ConfigScreen::resetThisServer))
                .option(resetButton("resetEverything", () -> OptionalInt.of(Ft10.get().resetAllScores())))
                .build())
            .category(ConfigCategory.createBuilder()
                .name(text("category.hud"))
                .group(OptionGroup.createBuilder()
                    .name(text("group.hud"))
                    .option(ButtonOption.createBuilder()
                        .name(text("editHud"))
                        .description(describe("editHud"))
                        .text(text("editHud.button"))
                        .action(screen -> ClientCompat.setScreen(Minecraft.getInstance(), new HudEditorScreen(screen)))
                        .build())
                    .option(bool("hudEnabled", d.hudEnabled, () -> c.hudEnabled, v -> c.hudEnabled = v))
                    .option(Option.<Ft10Config.HudVisibility>createBuilder()
                        .name(text("hudVisibility"))
                        .description(describe("hudVisibility"))
                        .binding(d.hudVisibility, () -> c.hudVisibility, v -> c.hudVisibility = v)
                        .controller(o -> EnumControllerBuilder.create(o)
                            .enumClass(Ft10Config.HudVisibility.class)
                            .formatValue(v -> text("hudVisibility." + v.name().toLowerCase())))
                        .build())
                    .option(slider("hudHideAfterSeconds", d.hudHideAfterSeconds, () -> c.hudHideAfterSeconds,
                        v -> c.hudHideAfterSeconds = v, 1, 120, 1))
                    .option(Option.<Float>createBuilder()
                        .name(text("hudScale"))
                        .description(describe("hudScale"))
                        .binding(d.hudScale, () -> c.hudScale, v -> c.hudScale = v)
                        .controller(o -> FloatSliderControllerBuilder.create(o).range(0.5f, 3.0f).step(0.05f))
                        .build())
                    .option(slider("hudOffsetX", d.hudOffsetX, () -> c.hudOffsetX, v -> c.hudOffsetX = v, -400, 400, 1))
                    .option(slider("hudOffsetY", d.hudOffsetY, () -> c.hudOffsetY, v -> c.hudOffsetY = v, -300, 60, 1))
                    .option(bool("hudShadow", d.hudShadow, () -> c.hudShadow, v -> c.hudShadow = v))
                    .option(bool("hudShowName", d.hudShowName, () -> c.hudShowName, v -> c.hudShowName = v))
                    .option(bool("hudShowGoal", d.hudShowGoal, () -> c.hudShowGoal, v -> c.hudShowGoal = v))
                    .build())
                .group(OptionGroup.createBuilder()
                    .name(text("group.colors"))
                    .option(color("colorMe", d.colorMe, () -> c.colorMe, v -> c.colorMe = v))
                    .option(color("colorThem", d.colorThem, () -> c.colorThem, v -> c.colorThem = v))
                    .option(color("colorSeparator", d.colorSeparator, () -> c.colorSeparator, v -> c.colorSeparator = v))
                    .build())
                .build())
            .category(ConfigCategory.createBuilder()
                .name(text("category.feedback"))
                .group(OptionGroup.createBuilder()
                    .name(text("group.title"))
                    .option(bool("titleEnabled", d.titleEnabled, () -> c.titleEnabled, v -> c.titleEnabled = v))
                    .option(slider("titleFadeIn", d.titleFadeIn, () -> c.titleFadeIn, v -> c.titleFadeIn = v, 0, 40, 1))
                    .option(slider("titleStay", d.titleStay, () -> c.titleStay, v -> c.titleStay = v, 1, 100, 1))
                    .option(slider("titleFadeOut", d.titleFadeOut, () -> c.titleFadeOut, v -> c.titleFadeOut = v, 0, 40, 1))
                    .build())
                .group(OptionGroup.createBuilder()
                    .name(text("group.chatSound"))
                    .option(bool("chatEnabled", d.chatEnabled, () -> c.chatEnabled, v -> c.chatEnabled = v))
                    .option(bool("soundEnabled", d.soundEnabled, () -> c.soundEnabled, v -> c.soundEnabled = v))
                    .option(Option.<Float>createBuilder()
                        .name(text("soundVolume"))
                        .description(describe("soundVolume"))
                        .binding(d.soundVolume, () -> c.soundVolume, v -> c.soundVolume = v)
                        .controller(o -> FloatSliderControllerBuilder.create(o).range(0f, 1f).step(0.05f))
                        .build())
                    .build())
                .build())
            .category(ConfigCategory.createBuilder()
                .name(text("category.detection"))
                .option(slider("combatTimeoutSeconds", d.combatTimeoutSeconds, () -> c.combatTimeoutSeconds,
                    v -> c.combatTimeoutSeconds = v, 1, 60, 1))
                .option(bool("switchOnHit", d.switchOnHit, () -> c.switchOnHit, v -> c.switchOnHit = v))
                .option(slider("switchLockSeconds", d.switchLockSeconds, () -> c.switchLockSeconds,
                    v -> c.switchLockSeconds = v, 0, 30, 1))
                .option(bool("usePresetPatterns", d.usePresetPatterns, () -> c.usePresetPatterns,
                    v -> c.usePresetPatterns = v))
                .group(ListOption.<String>createBuilder()
                    .name(text("customPatterns"))
                    .description(describe("customPatterns"))
                    .binding(new ArrayList<>(), () -> new ArrayList<>(c.customPatterns), v -> c.customPatterns = clean(v))
                    .controller(StringControllerBuilder::create)
                    .initial("{victim} was killed by {killer}")
                    .insertEntriesAtEnd(true)
                    .collapsed(c.customPatterns.isEmpty())
                    .build())
                .option(bool("countCombatLogs", d.countCombatLogs, () -> c.countCombatLogs,
                    v -> c.countCombatLogs = v))
                .option(bool("useSidebarStats", d.useSidebarStats, () -> c.useSidebarStats,
                    v -> c.useSidebarStats = v))
                .group(labels("statKillLabels", d.statKillLabels, () -> c.statKillLabels, v -> c.statKillLabels = v))
                .group(labels("statDeathLabels", d.statDeathLabels, () -> c.statDeathLabels,
                    v -> c.statDeathLabels = v))
                .build())
            .save(() -> Ft10.get().saveConfig())
            .build()
            .generateScreen(parent);
    }

    /** Drops blank lines and patterns that don't compile (they're reported in the log by the parser too). */
    private static List<String> clean(List<String> patterns) {
        List<String> out = new ArrayList<>();
        for (String p : patterns) {
            if (p != null && !p.isBlank() && DeathMessageParser.validate(p) == null) out.add(p);
        }
        return out;
    }

    /**
     * A red button that asks first, then runs {@code reset} (empty = not connected) and says in chat how many
     * scores went. Either way it comes back to the settings.
     */
    private static ButtonOption resetButton(String key, Supplier<OptionalInt> reset) {
        return ButtonOption.createBuilder()
            .name(text(key).copy().withColor(Texts.ERROR))
            .description(describe(key))
            .text(text(key + ".button"))
            .action(settings -> ClientCompat.setScreen(Minecraft.getInstance(), new ConfirmScreen(yes -> {
                if (yes) {
                    OptionalInt count = reset.get();
                    HudCompat.chat(Texts.line(count.isPresent()
                        ? Texts.colored(Texts.SUCCESS, "ft10.reset.all.done", Texts.value(count.getAsInt()))
                        : Texts.colored(Texts.ERROR, "ft10.error.not_connected")));
                }
                ClientCompat.setScreen(Minecraft.getInstance(), settings);
            }, text(key + ".confirm"), text(key + ".confirm.message"))))
            .build();
    }

    private static OptionalInt resetThisServer() {
        return Ft10.get().session().map(s -> OptionalInt.of(s.book().resetAll())).orElse(OptionalInt.empty());
    }

    /** A collapsed list of sidebar labels ("Kills", "Deaths"); blank entries are dropped. */
    private static ListOption<String> labels(String key, List<String> def, Supplier<List<String>> get,
                                             Consumer<List<String>> set) {
        return ListOption.<String>createBuilder()
            .name(text(key))
            .description(describe(key))
            .binding(new ArrayList<>(def), () -> new ArrayList<>(get.get()), v -> set.accept(nonBlank(v)))
            .controller(StringControllerBuilder::create)
            .initial("")
            .insertEntriesAtEnd(true)
            .collapsed(true)
            .build();
    }

    private static List<String> nonBlank(List<String> values) {
        List<String> out = new ArrayList<>();
        for (String v : values) if (v != null && !v.isBlank()) out.add(v.strip());
        return out;
    }

    private static Option<Boolean> bool(String key, boolean def, Supplier<Boolean> get, Consumer<Boolean> set) {
        return Option.<Boolean>createBuilder()
            .name(text(key))
            .description(describe(key))
            .binding(def, get, set)
            .controller(TickBoxControllerBuilder::create)
            .build();
    }

    private static Option<Integer> slider(String key, int def, Supplier<Integer> get, Consumer<Integer> set,
                                          int min, int max, int step) {
        return Option.<Integer>createBuilder()
            .name(text(key))
            .description(describe(key))
            .binding(def, get, set)
            .controller(o -> IntegerSliderControllerBuilder.create(o).range(min, max).step(step))
            .build();
    }

    private static Option<Color> color(String key, int def, Supplier<Integer> get, Consumer<Integer> set) {
        return Option.<Color>createBuilder()
            .name(text(key))
            .description(describe(key))
            .binding(new Color(def), () -> new Color(get.get()), v -> set.accept(v.getRGB() & 0xFFFFFF))
            .controller(ColorControllerBuilder::create)
            .build();
    }

    private static Component text(String key) {
        return Component.translatable("ft10.config." + key);
    }

    private static OptionDescription describe(String key) {
        return OptionDescription.of(Component.translatable("ft10.config." + key + ".desc"));
    }
}

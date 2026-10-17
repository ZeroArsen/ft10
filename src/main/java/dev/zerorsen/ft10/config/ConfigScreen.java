package dev.zerorsen.ft10.config;

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
import dev.zerorsen.ft10.core.DeathMessageParser;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
                .build())
            .category(ConfigCategory.createBuilder()
                .name(text("category.hud"))
                .group(OptionGroup.createBuilder()
                    .name(text("group.hud"))
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
                    .option(color("colorLabel", d.colorLabel, () -> c.colorLabel, v -> c.colorLabel = v))
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
                    .initial("^(?<victim>\\w+) was killed by (?<killer>\\w+)")
                    .insertEntriesAtEnd(true)
                    .collapsed(c.customPatterns.isEmpty())
                    .build())
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

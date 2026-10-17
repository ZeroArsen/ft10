package dev.zerorsen.ft10.feedback;

import dev.zerorsen.ft10.compat.TextCompat;
import dev.zerorsen.ft10.config.Ft10Config;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Chat and title building blocks in the RevoltPvP house style: white values inside gray text, green for good
 * news, red for bad, dark-gray framing, and a small-caps "ꜰᴛ |" tag in the logo yellow.
 */
public final class Texts {
    public static final int BRAND = 0xFFF200;
    public static final int BODY = 0x919191;
    public static final int VALUE = 0xFFFFFF;
    public static final int SUCCESS = 0x04E525;
    public static final int ERROR = 0xE50404;
    public static final int NOTICE = 0xF9E190;
    public static final int FRAME = 0x555555;

    private Texts() {}

    public static MutableComponent literal(Object text, int color) {
        return Component.literal(String.valueOf(text)).withColor(color);
    }

    /** "ꜰᴛ | " + content. */
    public static MutableComponent line(Component content) {
        return literal("ꜰᴛ", BRAND).append(literal(" | ", FRAME)).append(content);
    }

    public static MutableComponent body(String key, Object... args) {
        return Component.translatable(key, args).withColor(BODY);
    }

    public static MutableComponent colored(int color, String key, Object... args) {
        return Component.translatable(key, args).withColor(color);
    }

    public static MutableComponent value(Object value) {
        return literal(value, VALUE);
    }

    /** "4 - 7" in the player's colors. */
    public static MutableComponent score(int me, int them, Ft10Config config) {
        return literal(me, config.colorMe)
            .append(literal(" - ", config.colorSeparator))
            .append(literal(them, config.colorThem));
    }

    /** "(content)" with dark-gray brackets. */
    public static MutableComponent aside(Component content) {
        return literal("(", FRAME).append(content).append(literal(")", FRAME));
    }

    /** A clickable "[Label]" that runs a client command. */
    public static MutableComponent button(String labelKey, int color, String command, String hoverKey) {
        MutableComponent label = Component.translatable(labelKey).withColor(color);
        MutableComponent hover = body(hoverKey);
        return literal("[", FRAME).append(label).append(literal("]", FRAME))
            .withStyle(style -> TextCompat.hoverText(TextCompat.runCommand(style, command), hover));
    }

    public static MutableComponent bold(MutableComponent component) {
        return component.withStyle(style -> style.withBold(true));
    }
}

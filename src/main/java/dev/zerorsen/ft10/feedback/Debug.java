package dev.zerorsen.ft10.feedback;

import dev.zerorsen.ft10.Ft10Client;
import dev.zerorsen.ft10.compat.HudCompat;
import dev.zerorsen.ft10.compat.TextCompat;
import dev.zerorsen.ft10.core.PlayerRef;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * {@code /ft debug}: explains every kill signal in chat (only the player sees it) and in the log, and keeps the
 * last messages that mentioned a fight but matched no pattern, so a missing server format can be reported.
 */
public final class Debug {
    private static final int KEEP = 10;
    private static final int MAX_TEXT = 256;

    /** @param channelKey lang key of where the text came from (chat, action bar, title, …) */
    public record Unrecognized(String channelKey, String text) {}

    /** Dev runs can start with debug on: {@code -Pft10.debug} (see build.gradle.kts). */
    private boolean enabled = Boolean.getBoolean("ft10.debug");
    private final Deque<Unrecognized> unrecognized = new ArrayDeque<>();

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** A gray "ᴅᴇʙᴜɢ …" chat line; {@code args} are shown as white values. */
    public void signal(String key, Object... args) {
        if (!enabled) return;
        Object[] values = new Object[args.length];
        for (int i = 0; i < args.length; i++) values[i] = value(args[i]);
        MutableComponent line = Texts.literal("ᴅᴇʙᴜɢ ", Texts.NOTICE).append(Texts.body(key, values));
        HudCompat.chat(Texts.line(line));
        Ft10Client.LOGGER.info("[debug] {}", line.getString());
    }

    /** A message that looked like it was about a fight but matched nothing. Remembered even with debug off. */
    public void unrecognized(String channelKey, String text) {
        String clipped = text.length() > MAX_TEXT ? text.substring(0, MAX_TEXT) : text;
        Unrecognized entry = new Unrecognized(channelKey, clipped);
        if (entry.equals(unrecognized.peekLast())) return;
        unrecognized.addLast(entry);
        while (unrecognized.size() > KEEP) unrecognized.removeFirst();
        if (!enabled) return;
        HudCompat.chat(Texts.line(describe(entry)));
        Ft10Client.LOGGER.info("[debug] not recognized ({}): {}", channelKey, clipped);
    }

    public List<Unrecognized> recent() {
        return new ArrayList<>(unrecognized);
    }

    /** "ᴅᴇʙᴜɢ Not recognized (chat): … [Copy]". */
    public static MutableComponent describe(Unrecognized entry) {
        MutableComponent copy = Texts.literal("[", Texts.FRAME)
            .append(Component.translatable("ft10.debug.copy").withColor(Texts.NOTICE))
            .append(Texts.literal("]", Texts.FRAME))
            .withStyle(style -> TextCompat.hoverText(TextCompat.copyToClipboard(style, entry.text()),
                Texts.body("ft10.debug.copy.hover")));
        return Texts.literal("ᴅᴇʙᴜɢ ", Texts.NOTICE)
            .append(Texts.body("ft10.debug.unrecognized", Component.translatable(entry.channelKey()),
                Texts.value(entry.text())))
            .append(" ")
            .append(copy);
    }

    private static Object value(Object arg) {
        if (arg instanceof Component component) return component;
        if (arg instanceof PlayerRef player) return Texts.value(player.name());
        if (arg == null) return Texts.literal("—", Texts.FRAME);
        return Texts.value(arg);
    }
}

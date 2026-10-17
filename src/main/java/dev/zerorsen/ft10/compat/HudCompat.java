package dev.zerorsen.ft10.compat;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Everything the mod touches on the in-game HUD. This is the part of Minecraft that changes most between
 * versions (HUD element registry in 1.21.6, Gui split into Gui + Hud and GuiGraphics renamed to
 * GuiGraphicsExtractor in 26.x), so it all lives here behind a small {@link Canvas}.
 */
public final class HudCompat {
    private HudCompat() {}

    /** The few drawing calls the score HUD needs. */
    public interface Canvas {
        int width();

        int height();

        int textWidth(Component text);

        /** Draws text; colors come from the component's style. */
        void text(Component text, int x, int y, boolean shadow);

        void push();

        void translate(float x, float y);

        void scale(float factor);

        void pop();
    }

    /** Draws {@code renderer} right after the vanilla action bar. */
    public static void register(Identifier id, Consumer<Canvas> renderer) {
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE, id,
            (graphics, deltaTracker) -> renderer.accept(new GraphicsCanvas(graphics)));
    }

    public static void title(Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        Hud hud = hud();
        hud.setTimes(fadeIn, stay, fadeOut);
        hud.setSubtitle(subtitle);
        hud.setTitle(title);
    }

    /** Adds a line to the chat box locally. Nothing is sent to the server. */
    public static void chat(Component message) {
        hud().getChat().addClientSystemMessage(message);
    }

    /** F1 (hide GUI) is on. */
    public static boolean hidden() {
        return hud().isHidden();
    }

    private static Hud hud() {
        return Minecraft.getInstance().gui.hud;
    }

    private record GraphicsCanvas(GuiGraphicsExtractor graphics) implements Canvas {
        @Override public int width() { return graphics.guiWidth(); }
        @Override public int height() { return graphics.guiHeight(); }
        @Override public int textWidth(Component text) { return Minecraft.getInstance().font.width(text); }

        @Override
        public void text(Component text, int x, int y, boolean shadow) {
            // Since 1.21.6 the color is ARGB: an alpha of 0 would draw nothing.
            graphics.text(Minecraft.getInstance().font, text, x, y, 0xFFFFFFFF, shadow);
        }

        @Override public void push() { graphics.pose().pushMatrix(); }
        @Override public void translate(float x, float y) { graphics.pose().translate(x, y); }
        @Override public void scale(float factor) { graphics.pose().scale(factor, factor); }
        @Override public void pop() { graphics.pose().popMatrix(); }
    }
}

package dev.zerorsen.ft10.compat;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
//?} else {
/*import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
*///?}
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

        /** Solid rectangle, ARGB color. */
        void fill(int x0, int y0, int x1, int y1, int argb);

        /** One-pixel rectangle border, ARGB color. */
        void outline(int x0, int y0, int x1, int y1, int argb);
    }

    /** Draws {@code renderer} right after the vanilla action bar. */
    public static void register(Identifier id, Consumer<Canvas> renderer) {
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE, id,
            (graphics, deltaTracker) -> renderer.accept(canvas(graphics)));
    }

    public static void title(Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        var hud = hud();
        hud.setTimes(fadeIn, stay, fadeOut);
        hud.setSubtitle(subtitle);
        hud.setTitle(title);
    }

    /** Wraps the version's GUI graphics object (for {@link CompatScreen}). */
    //? if >=26.1 {
    static Canvas canvas(GuiGraphicsExtractor graphics) {
    //?} else {
    /*static Canvas canvas(GuiGraphics graphics) {
    *///?}
        return new GraphicsCanvas(graphics);
    }

    /** A short message in the vanilla action bar (used to confirm key presses). */
    public static void actionBar(Component message) {
        hud().setOverlayMessage(message, false);
    }

    /** Adds a line to the chat box locally. Nothing is sent to the server. */
    public static void chat(Component message) {
        //? if >=26.1 {
        hud().getChat().addClientSystemMessage(message);
        //?} else {
        /*hud().getChat().addMessage(message);
        *///?}
    }

    /** F1 (hide GUI) is on. */
    public static boolean hidden() {
        //? if >=26.1 {
        return hud().isHidden();
        //?} else {
        /*return Minecraft.getInstance().options.hideGui;
        *///?}
    }

    /** The title, action bar and chat: on {@code Gui.hud} since 26.x, on {@code Gui} itself before. */
    //? if >=26.1 {
    private static Hud hud() {
        return Minecraft.getInstance().gui.hud;
    }
    //?} else {
    /*private static Gui hud() {
        return Minecraft.getInstance().gui;
    }
    *///?}

    //? if >=26.1 {
    private record GraphicsCanvas(GuiGraphicsExtractor graphics) implements Canvas {
    //?} else {
    /*private record GraphicsCanvas(GuiGraphics graphics) implements Canvas {
    *///?}
        @Override public int width() { return graphics.guiWidth(); }
        @Override public int height() { return graphics.guiHeight(); }
        @Override public int textWidth(Component text) { return Minecraft.getInstance().font.width(text); }

        /** Since 1.21.6 the color is ARGB: an alpha of 0 would draw nothing. */
        @Override
        public void text(Component text, int x, int y, boolean shadow) {
            //? if >=26.1 {
            graphics.text(Minecraft.getInstance().font, text, x, y, 0xFFFFFFFF, shadow);
            //?} else {
            /*graphics.drawString(Minecraft.getInstance().font, text, x, y, 0xFFFFFFFF, shadow);
            *///?}
        }

        @Override public void push() { graphics.pose().pushMatrix(); }
        @Override public void translate(float x, float y) { graphics.pose().translate(x, y); }
        @Override public void scale(float factor) { graphics.pose().scale(factor, factor); }
        @Override public void pop() { graphics.pose().popMatrix(); }

        @Override
        public void fill(int x0, int y0, int x1, int y1, int argb) {
            graphics.fill(x0, y0, x1, y1, argb);
        }

        @Override
        public void outline(int x0, int y0, int x1, int y1, int argb) {
            //? if >=26.1 {
            graphics.outline(x0, y0, x1 - x0, y1 - y0, argb);
            //?} else {
            /*graphics.renderOutline(x0, y0, x1 - x0, y1 - y0, argb);
            *///?}
        }
    }
}

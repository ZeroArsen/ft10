package dev.zerorsen.ft10.compat;

//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}
import net.minecraft.client.gui.screens.Screen;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import net.minecraft.network.chat.Component;

/**
 * A screen with version-neutral hooks. Drawing and input methods changed shape across versions (GuiGraphics →
 * GuiGraphicsExtractor in 26.x, mouse/key event records in 1.21.9), so subclasses only implement the hooks below
 * and this class maps them onto whatever the current version calls.
 */
public abstract class CompatScreen extends Screen {
    protected CompatScreen(Component title) {
        super(title);
    }

    /** Draws the screen's own content; widgets (buttons) are drawn on top afterwards. */
    protected abstract void draw(HudCompat.Canvas canvas, int mouseX, int mouseY);

    protected boolean press(double x, double y, int button) { return false; }

    protected boolean drag(double x, double y, int button) { return false; }

    protected boolean release(double x, double y, int button) { return false; }

    protected boolean scroll(double x, double y, double amount) { return false; }

    protected boolean key(int keyCode, boolean shift) { return false; }

    /** Keep the world visible behind the screen when we're in one; otherwise the normal menu background. */
    protected boolean showWorld() {
        return minecraft.level != null;
    }

    //? if >=26.1 {
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!showWorld()) super.extractBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        draw(HudCompat.canvas(graphics), mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
    //?} else {
    /*@Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!showWorld()) super.renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        draw(HudCompat.canvas(graphics), mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    *///?}

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick) || press(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return drag(event.x(), event.y(), event.button()) || super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        boolean handled = release(event.x(), event.y(), event.button());
        return super.mouseReleased(event) || handled;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        boolean shift = (event.modifiers() & 1) != 0; // GLFW_MOD_SHIFT
        return key(event.key(), shift) || super.keyPressed(event);
    }
    //?} else {
    /*@Override
    public boolean mouseClicked(double x, double y, int button) {
        return super.mouseClicked(x, y, button) || press(x, y, button);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return drag(x, y, button) || super.mouseDragged(x, y, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        boolean handled = release(x, y, button);
        return super.mouseReleased(x, y, button) || handled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean shift = (modifiers & 1) != 0;
        return key(keyCode, shift) || super.keyPressed(keyCode, scanCode, modifiers);
    }
    *///?}

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        return scroll(x, y, scrollY) || super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

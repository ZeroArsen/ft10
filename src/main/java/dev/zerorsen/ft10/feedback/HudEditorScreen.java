package dev.zerorsen.ft10.feedback;

import com.mojang.blaze3d.platform.InputConstants;
import dev.zerorsen.ft10.Ft10;
import dev.zerorsen.ft10.compat.ClientCompat;
import dev.zerorsen.ft10.compat.CompatScreen;
import dev.zerorsen.ft10.compat.HudCompat;
import dev.zerorsen.ft10.config.Ft10Config;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Move and resize the score HUD by hand: drag it, scroll to resize, arrow keys to nudge (Shift = 10 px),
 * R to reset. Esc or Done saves; Cancel restores the previous position.
 */
public final class HudEditorScreen extends CompatScreen {
    private static final ScoreHud.View SAMPLE = new ScoreHud.View("Opponent", 4, 7, 1, 0);
    private static final int SNAP_PX = 4;
    private static final int BUTTON_WIDTH = 80;

    private final Screen parent;
    private final Ft10Config config;
    private final int startX;
    private final int startY;
    private final float startScale;
    private ScoreHud.Bounds bounds;
    private boolean dragging;
    private double grabX;
    private double grabY;

    public HudEditorScreen(Screen parent) {
        super(Component.translatable("ft10.editor.title"));
        this.parent = parent;
        this.config = Ft10.get().config();
        this.startX = config.hudOffsetX;
        this.startY = config.hudOffsetY;
        this.startScale = config.hudScale;
    }

    @Override
    protected void init() {
        int y = 24;
        int x = width / 2 - (3 * BUTTON_WIDTH + 8) / 2;
        addRenderableWidget(Button.builder(Component.translatable("ft10.editor.done"), b -> onClose())
            .bounds(x, y, BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("ft10.editor.reset"), b -> resetPosition())
            .bounds(x + BUTTON_WIDTH + 4, y, BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("ft10.editor.cancel"), b -> cancel())
            .bounds(x + 2 * (BUTTON_WIDTH + 4), y, BUTTON_WIDTH, 20).build());
    }

    @Override
    protected void draw(HudCompat.Canvas canvas, int mouseX, int mouseY) {
        Component help = Component.translatable("ft10.editor.help").withColor(Texts.VALUE);
        canvas.text(help, (canvas.width() - canvas.textWidth(help)) / 2, 8, true);

        if (config.hudOffsetX == 0) {
            canvas.fill(canvas.width() / 2, 50, canvas.width() / 2 + 1, canvas.height(), 0x40FFFFFF); // center guide
        }
        bounds = ScoreHud.draw(canvas, ScoreHud.activeView().orElse(SAMPLE), config);
        boolean hot = dragging || bounds.contains(mouseX, mouseY);
        canvas.fill(bounds.x0(), bounds.y0(), bounds.x1(), bounds.y1(), hot ? 0x30FFF200 : 0x18FFFFFF);
        canvas.outline(bounds.x0(), bounds.y0(), bounds.x1(), bounds.y1(), hot ? 0xFFFFF200 : 0x80FFFFFF);

        Component info = Component.literal(String.format("x %+d  y %+d  ×%.2f", config.hudOffsetX, config.hudOffsetY,
            config.hudScale)).withColor(Texts.BODY);
        canvas.text(info, (canvas.width() - canvas.textWidth(info)) / 2, 48, true);
    }

    @Override
    protected boolean press(double x, double y, int button) {
        if (button != 0 || bounds == null || !bounds.contains(x, y)) return false;
        dragging = true;
        grabX = x - (width / 2.0 + config.hudOffsetX);
        grabY = y - (ScoreHud.defaultAnchorY(height) + config.hudOffsetY);
        return true;
    }

    @Override
    protected boolean drag(double x, double y, int button) {
        if (!dragging) return false;
        int offsetX = (int) Math.round(x - grabX - width / 2.0);
        int offsetY = (int) Math.round(y - grabY - ScoreHud.defaultAnchorY(height));
        if (Math.abs(offsetX) <= SNAP_PX) offsetX = 0; // snap to the center line
        setOffset(offsetX, offsetY);
        return true;
    }

    @Override
    protected boolean release(double x, double y, int button) {
        boolean was = dragging;
        dragging = false;
        return was;
    }

    @Override
    protected boolean scroll(double x, double y, double amount) {
        float scale = Math.round((config.hudScale + (float) amount * 0.05f) * 20f) / 20f;
        config.hudScale = Math.max(0.5f, Math.min(3.0f, scale));
        return true;
    }

    @Override
    protected boolean key(int keyCode, boolean shift) {
        int step = shift ? 10 : 1;
        switch (keyCode) {
            case InputConstants.KEY_LEFT -> setOffset(config.hudOffsetX - step, config.hudOffsetY);
            case InputConstants.KEY_RIGHT -> setOffset(config.hudOffsetX + step, config.hudOffsetY);
            case InputConstants.KEY_UP -> setOffset(config.hudOffsetX, config.hudOffsetY - step);
            case InputConstants.KEY_DOWN -> setOffset(config.hudOffsetX, config.hudOffsetY + step);
            case InputConstants.KEY_R -> resetPosition();
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Keeps the HUD on screen. */
    private void setOffset(int offsetX, int offsetY) {
        int anchorY = ScoreHud.defaultAnchorY(height);
        config.hudOffsetX = Math.max(-width / 2, Math.min(width / 2, offsetX));
        config.hudOffsetY = Math.max(-anchorY + 10, Math.min(height - anchorY, offsetY));
    }

    private void resetPosition() {
        Ft10Config defaults = new Ft10Config();
        config.hudOffsetX = defaults.hudOffsetX;
        config.hudOffsetY = defaults.hudOffsetY;
        config.hudScale = defaults.hudScale;
    }

    private void cancel() {
        config.hudOffsetX = startX;
        config.hudOffsetY = startY;
        config.hudScale = startScale;
        ClientCompat.setScreen(minecraft, parent);
    }

    /** Esc and Done: keep the new position. */
    @Override
    public void onClose() {
        Ft10.get().saveConfig();
        ClientCompat.setScreen(minecraft, parent);
    }
}

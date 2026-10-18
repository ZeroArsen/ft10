package dev.zerorsen.ft10.feedback;

import com.mojang.blaze3d.platform.InputConstants;
import dev.zerorsen.ft10.Ft10;
import dev.zerorsen.ft10.compat.ClientCompat;
import dev.zerorsen.ft10.compat.CompatScreen;
import dev.zerorsen.ft10.compat.HudCompat;
import dev.zerorsen.ft10.config.Ft10Config;
import java.util.UUID;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Move and resize the score HUD by hand: drag it, scroll or +/− (keys or buttons) to resize, arrow keys to nudge
 * (Shift = 10 px), R to reset. A button turns the player heads on and off. Esc or Done saves; Cancel restores the
 * previous position, size and heads setting.
 */
public final class HudEditorScreen extends CompatScreen {
    private static final ScoreHud.View SAMPLE = new ScoreHud.View(new UUID(0L, 2L), "Opponent", 4, 7, 1, 0);
    private static final int SNAP_PX = 4;
    private static final int BUTTON_WIDTH = 80;
    private static final int SMALL_BUTTON = 20;
    /** Same limits as {@code Ft10Config.sanitize}. */
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 3.0f;

    private final Screen parent;
    private final Ft10Config config;
    private final int startX;
    private final int startY;
    private final float startScale;
    private final boolean startHeads;
    private Button sizeLabel;
    private Button headsButton;
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
        this.startHeads = config.hudShowHeads;
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

        // Second row: [−] Size ×1.00 [+]   [Heads: On]
        y += 24;
        x = width / 2 - (2 * SMALL_BUTTON + 2 * BUTTON_WIDTH + 16) / 2;
        addRenderableWidget(Button.builder(Component.literal("−"), b -> setScale(config.hudScale - 0.1f))
            .bounds(x, y, SMALL_BUTTON, 20).build());
        sizeLabel = addRenderableWidget(Button.builder(Component.empty(), b -> {})
            .bounds(x + SMALL_BUTTON + 4, y, BUTTON_WIDTH, 20).build());
        sizeLabel.active = false;
        addRenderableWidget(Button.builder(Component.literal("+"), b -> setScale(config.hudScale + 0.1f))
            .bounds(x + SMALL_BUTTON + BUTTON_WIDTH + 8, y, SMALL_BUTTON, 20).build());
        headsButton = addRenderableWidget(Button.builder(Component.empty(), b -> {
                config.hudShowHeads = !config.hudShowHeads;
                updateLabels();
            })
            .bounds(x + 2 * SMALL_BUTTON + BUTTON_WIDTH + 16, y, BUTTON_WIDTH, 20).build());
        updateLabels();
    }

    private void updateLabels() {
        if (sizeLabel == null) return;
        sizeLabel.setMessage(Component.translatable("ft10.editor.size", String.format("%.2f", config.hudScale)));
        headsButton.setMessage(Component.translatable(config.hudShowHeads ? "ft10.editor.heads.on" : "ft10.editor.heads.off"));
    }

    @Override
    protected void draw(HudCompat.Canvas canvas, int mouseX, int mouseY) {
        Component help = Component.translatable("ft10.editor.help").withColor(Texts.VALUE);
        canvas.text(help, (canvas.width() - canvas.textWidth(help)) / 2, 8, true);

        if (config.hudOffsetX == 0) {
            canvas.fill(canvas.width() / 2, 86, canvas.width() / 2 + 1, canvas.height(), 0x40FFFFFF); // center guide
        }
        bounds = ScoreHud.draw(canvas, ScoreHud.activeView().orElse(SAMPLE), config);
        boolean hot = dragging || bounds.contains(mouseX, mouseY);
        canvas.fill(bounds.x0(), bounds.y0(), bounds.x1(), bounds.y1(), hot ? 0x30FFF200 : 0x18FFFFFF);
        canvas.outline(bounds.x0(), bounds.y0(), bounds.x1(), bounds.y1(), hot ? 0xFFFFF200 : 0x80FFFFFF);

        Component info = Component.literal(String.format("x %+d  y %+d  ×%.2f", config.hudOffsetX, config.hudOffsetY,
            config.hudScale)).withColor(Texts.BODY);
        canvas.text(info, (canvas.width() - canvas.textWidth(info)) / 2, 74, true);
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
        setScale(config.hudScale + (float) amount * 0.05f);
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
            case InputConstants.KEY_EQUALS -> setScale(config.hudScale + 0.05f);
            case InputConstants.KEY_MINUS -> setScale(config.hudScale - 0.05f);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Rounds to 0.05 and keeps the size within the config limits. */
    private void setScale(float scale) {
        float rounded = Math.round(scale * 20f) / 20f;
        config.hudScale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, rounded));
        updateLabels();
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
        updateLabels();
    }

    private void cancel() {
        config.hudOffsetX = startX;
        config.hudOffsetY = startY;
        config.hudScale = startScale;
        config.hudShowHeads = startHeads;
        ClientCompat.setScreen(minecraft, parent);
    }

    /** Esc and Done: keep the new position. */
    @Override
    public void onClose() {
        Ft10.get().saveConfig();
        ClientCompat.setScreen(minecraft, parent);
    }
}

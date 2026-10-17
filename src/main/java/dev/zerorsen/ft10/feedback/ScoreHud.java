package dev.zerorsen.ft10.feedback;

import dev.zerorsen.ft10.Ft10;
import dev.zerorsen.ft10.compat.ClientCompat;
import dev.zerorsen.ft10.compat.HudCompat;
import dev.zerorsen.ft10.config.Ft10Config;
import dev.zerorsen.ft10.core.Rivalry;
import dev.zerorsen.ft10.core.Session;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Draws the score just above the vanilla action bar:
 * <pre>
 *     vs Alex · FT10
 *         4 - 7
 *  [server action bar text]
 * </pre>
 * It is our own HUD element, so server action bar messages never overwrite it.
 */
public final class ScoreHud {
    /** Vanilla draws the action bar text from height - 72 down; we keep a 3 px gap above it. */
    private static final int BOTTOM_FROM_SCREEN_BOTTOM = 75;
    private static final int LINE_HEIGHT = 10;

    /** What the HUD shows: the active rivalry in game, sample data in the HUD editor. */
    public record View(String name, int me, int them, int setsMe, int setsThem) {
        static View of(Rivalry r) {
            return new View(r.name(), r.me(), r.them(), r.setsMe(), r.setsThem());
        }

        boolean hasSets() {
            return setsMe > 0 || setsThem > 0;
        }
    }

    /** Screen rectangle the HUD covers, for dragging it in the editor. */
    public record Bounds(int x0, int y0, int x1, int y1) {
        public boolean contains(double x, double y) {
            return x >= x0 && x <= x1 && y >= y0 && y <= y1;
        }
    }

    private ScoreHud() {}

    /** The in-game HUD element. */
    public static void render(HudCompat.Canvas canvas) {
        Ft10 ft10 = Ft10.get();
        Ft10Config c = ft10.config();
        if (!c.enabled || !c.hudEnabled || HudCompat.hidden()) return;
        if (ClientCompat.screen(Minecraft.getInstance()) instanceof HudEditorScreen) return; // it draws its own

        Optional<Session> session = ft10.session();
        if (session.isEmpty()) return;
        Optional<Rivalry> active = session.get().book().active();
        if (active.isEmpty()) return;
        if (c.hudVisibility == Ft10Config.HudVisibility.IN_COMBAT
            && System.currentTimeMillis() - session.get().lastActivity() > c.hudHideAfterSeconds * 1000L) {
            return;
        }
        draw(canvas, View.of(active.get()), c);
    }

    /** The active rivalry, if any; used by the editor so the preview shows real names. */
    static Optional<View> activeView() {
        return Ft10.get().session().flatMap(s -> s.book().active()).map(View::of);
    }

    /** Draws {@code view} where the config puts it and returns the area it covers. */
    public static Bounds draw(HudCompat.Canvas canvas, View view, Ft10Config c) {
        Component score = Texts.bold(Texts.score(view.me(), view.them(), c));
        Component label = label(view, c);

        canvas.push();
        canvas.translate(anchorX(canvas, c), anchorY(canvas, c));
        canvas.scale(c.hudScale);
        canvas.text(score, -canvas.textWidth(score) / 2, -LINE_HEIGHT + 1, c.hudShadow);
        if (label != null) {
            canvas.text(label, -canvas.textWidth(label) / 2, -2 * LINE_HEIGHT + 1, c.hudShadow);
        }
        canvas.pop();
        return bounds(canvas, score, label, c);
    }

    private static Bounds bounds(HudCompat.Canvas canvas, Component score, Component label, Ft10Config c) {
        int widest = Math.max(canvas.textWidth(score), label == null ? 0 : canvas.textWidth(label));
        int lines = label == null ? 1 : 2;
        float halfWidth = widest * c.hudScale / 2f;
        float height = lines * LINE_HEIGHT * c.hudScale;
        float x = anchorX(canvas, c);
        float y = anchorY(canvas, c);
        return new Bounds(Math.round(x - halfWidth) - 2, Math.round(y - height) - 1,
            Math.round(x + halfWidth) + 2, Math.round(y) + 1);
    }

    /** Horizontal center of the HUD. */
    static float anchorX(HudCompat.Canvas canvas, Ft10Config c) {
        return canvas.width() / 2f + c.hudOffsetX;
    }

    /** Bottom edge of the HUD. */
    static float anchorY(HudCompat.Canvas canvas, Ft10Config c) {
        return canvas.height() - BOTTOM_FROM_SCREEN_BOTTOM + c.hudOffsetY;
    }

    static int defaultAnchorY(int screenHeight) {
        return screenHeight - BOTTOM_FROM_SCREEN_BOTTOM;
    }

    /** "vs Alex · FT10 · 1 - 0" — whichever parts are enabled; null if none. */
    private static Component label(View view, Ft10Config c) {
        MutableComponent label = Component.empty();
        boolean any = false;
        if (c.hudShowName) {
            label.append(Component.translatable("ft10.hud.vs", Texts.literal(view.name(), Texts.VALUE)).withColor(c.colorLabel));
            any = true;
        }
        if (c.hudShowGoal && c.goal > 0) {
            if (any) label.append(Texts.literal(" · ", c.colorSeparator));
            label.append(Texts.literal("FT" + c.goal, Texts.BRAND));
            if (view.hasSets()) {
                label.append(Texts.literal(" · ", c.colorSeparator)).append(Texts.score(view.setsMe(), view.setsThem(), c));
            }
            any = true;
        }
        return any ? label : null;
    }
}

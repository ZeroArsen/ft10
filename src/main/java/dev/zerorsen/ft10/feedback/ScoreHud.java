package dev.zerorsen.ft10.feedback;

import dev.zerorsen.ft10.Ft10;
import dev.zerorsen.ft10.compat.HudCompat;
import dev.zerorsen.ft10.config.Ft10Config;
import dev.zerorsen.ft10.core.Rivalry;
import dev.zerorsen.ft10.core.Session;
import java.util.Optional;
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

    private ScoreHud() {}

    public static void render(HudCompat.Canvas canvas) {
        Ft10 ft10 = Ft10.get();
        Ft10Config c = ft10.config();
        if (!c.enabled || !c.hudEnabled || HudCompat.hidden()) return;

        Optional<Session> session = ft10.session();
        if (session.isEmpty()) return;
        Optional<Rivalry> active = session.get().book().active();
        if (active.isEmpty()) return;
        if (c.hudVisibility == Ft10Config.HudVisibility.IN_COMBAT
            && System.currentTimeMillis() - session.get().lastActivity() > c.hudHideAfterSeconds * 1000L) {
            return;
        }

        Rivalry r = active.get();
        Component score = Texts.bold(Texts.score(r.me(), r.them(), c));
        Component label = label(r, c);

        canvas.push();
        canvas.translate(canvas.width() / 2f + c.hudOffsetX, canvas.height() - BOTTOM_FROM_SCREEN_BOTTOM + c.hudOffsetY);
        canvas.scale(c.hudScale);
        canvas.text(score, -canvas.textWidth(score) / 2, -LINE_HEIGHT + 1, c.hudShadow);
        if (label != null) {
            canvas.text(label, -canvas.textWidth(label) / 2, -2 * LINE_HEIGHT + 1, c.hudShadow);
        }
        canvas.pop();
    }

    /** "vs Alex · FT10 · 1 - 0" — whichever parts are enabled; null if none. */
    private static Component label(Rivalry r, Ft10Config c) {
        MutableComponent label = Component.empty();
        boolean any = false;
        if (c.hudShowName) {
            label.append(Component.translatable("ft10.hud.vs", Texts.literal(r.name(), Texts.VALUE)).withColor(c.colorLabel));
            any = true;
        }
        if (c.hudShowGoal && c.goal > 0) {
            if (any) label.append(Texts.literal(" · ", c.colorSeparator));
            label.append(Texts.literal("FT" + c.goal, Texts.BRAND));
            if (r.hasSets()) {
                label.append(Texts.literal(" · ", c.colorSeparator)).append(Texts.score(r.setsMe(), r.setsThem(), c));
            }
            any = true;
        }
        return any ? label : null;
    }
}

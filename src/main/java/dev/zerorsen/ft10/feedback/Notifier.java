package dev.zerorsen.ft10.feedback;

import dev.zerorsen.ft10.compat.HudCompat;
import dev.zerorsen.ft10.config.Ft10Config;
import dev.zerorsen.ft10.core.Rivalry;
import dev.zerorsen.ft10.core.ScoreUpdate;
import dev.zerorsen.ft10.core.Session;
import dev.zerorsen.ft10.core.Side;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/** Title, local chat line and sound for each point, like a duel server's round messages. */
public final class Notifier implements Session.Listener {
    public static final String UNDO_COMMAND = "/ft10 undo";

    private final Supplier<Ft10Config> config;

    public Notifier(Supplier<Ft10Config> config) {
        this.config = config;
    }

    @Override
    public void onScore(ScoreUpdate update) {
        Ft10Config c = config.get();
        if (update.set() != null) setFinished(update, c);
        else point(update, c);
    }

    @Override
    public void onActiveChanged(Rivalry rivalry) {
        // The HUD reads the active rivalry every frame; nothing to push here.
    }

    private void point(ScoreUpdate update, Ft10Config c) {
        boolean mine = update.scorer() == Side.ME;
        int color = mine ? Texts.SUCCESS : Texts.ERROR;
        Component name = Texts.value(update.rivalry().name());

        if (c.titleEnabled) {
            Component title = Texts.bold(Texts.score(update.me(), update.them(), c));
            Component subtitle = Texts.colored(color, mine ? "ft10.subtitle.kill" : "ft10.subtitle.death", name);
            HudCompat.title(title, subtitle, c.titleFadeIn, c.titleStay, c.titleFadeOut);
        }
        if (c.chatEnabled) {
            MutableComponent line = Texts.literal("+1 ", color)
                .append(Texts.body(mine ? "ft10.chat.kill" : "ft10.chat.death", name))
                .append(" ")
                .append(Texts.aside(Texts.score(update.me(), update.them(), c)))
                .append(" ")
                .append(Texts.button("ft10.chat.undo", Texts.NOTICE, UNDO_COMMAND, "ft10.chat.undo.hover"));
            HudCompat.chat(Texts.line(line));
        }
        if (mine) play(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, c);
        else play(SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f, c);
    }

    private void setFinished(ScoreUpdate update, Ft10Config c) {
        ScoreUpdate.SetResult set = update.set();
        boolean won = set.winner() == Side.ME;
        int color = won ? Texts.SUCCESS : Texts.ERROR;
        Component goal = Texts.literal("FT" + set.goal(), Texts.BRAND);
        Component finalScore = Texts.score(set.finalMe(), set.finalThem(), c);
        Component sets = Texts.body("ft10.sets", Texts.score(set.setsMe(), set.setsThem(), c));

        if (c.titleEnabled) {
            Component title = Texts.bold(Texts.colored(color, won ? "ft10.title.victory" : "ft10.title.defeat"));
            Component subtitle = Component.empty().append(goal).append(Texts.literal(" · ", Texts.FRAME))
                .append(finalScore);
            HudCompat.title(title, subtitle, c.titleFadeIn, c.titleStay + 20, c.titleFadeOut);
        }
        if (c.chatEnabled) {
            MutableComponent line = Texts.colored(color, won ? "ft10.chat.set_won" : "ft10.chat.set_lost",
                    goal, Texts.value(update.rivalry().name()))
                .append(" ")
                .append(Texts.aside(finalScore))
                .append(Texts.literal(" | ", Texts.FRAME))
                .append(sets)
                .append(" ")
                .append(Texts.button("ft10.chat.undo", Texts.NOTICE, UNDO_COMMAND, "ft10.chat.undo.hover"));
            HudCompat.chat(Texts.line(line));
        }
        if (won) play(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, c);
        else play(SoundEvents.NOTE_BLOCK_BASS.value(), 0.5f, c);
    }

    private static void play(SoundEvent sound, float pitch, Ft10Config c) {
        if (!c.soundEnabled || c.soundVolume <= 0f) return;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, c.soundVolume));
    }
}

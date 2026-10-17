package dev.zerorsen.ft10;

import dev.zerorsen.ft10.compat.ClientCompat;
import dev.zerorsen.ft10.compat.HudCompat;
import dev.zerorsen.ft10.compat.KeyCompat;
import dev.zerorsen.ft10.config.ConfigScreen;
import dev.zerorsen.ft10.config.Ft10Config;
import dev.zerorsen.ft10.core.Rivalry;
import dev.zerorsen.ft10.core.ScoreBook;
import dev.zerorsen.ft10.core.Session;
import dev.zerorsen.ft10.feedback.HudEditorScreen;
import dev.zerorsen.ft10.feedback.Texts;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Optional hotkeys (all unbound by default). Each press is confirmed in the vanilla action bar. */
final class Keybinds {
    private static KeyMapping toggle;
    private static KeyMapping toggleHud;
    private static KeyMapping undo;
    private static KeyMapping reset;
    private static KeyMapping settings;
    private static KeyMapping editHud;

    private Keybinds() {}

    static void register() {
        toggle = KeyCompat.register("key.ft10.toggle");
        toggleHud = KeyCompat.register("key.ft10.toggle_hud");
        undo = KeyCompat.register("key.ft10.undo");
        reset = KeyCompat.register("key.ft10.reset");
        settings = KeyCompat.register("key.ft10.settings");
        editHud = KeyCompat.register("key.ft10.edit_hud");
    }

    static void tick(Minecraft minecraft) {
        Ft10 ft10 = Ft10.get();
        Ft10Config c = ft10.config();
        while (toggle.consumeClick()) {
            c.enabled = !c.enabled;
            ft10.saveConfig();
            HudCompat.actionBar(Texts.colored(c.enabled ? Texts.SUCCESS : Texts.ERROR,
                c.enabled ? "ft10.state.enabled" : "ft10.state.disabled"));
        }
        while (toggleHud.consumeClick()) {
            c.hudEnabled = !c.hudEnabled;
            ft10.saveConfig();
            HudCompat.actionBar(Texts.body(c.hudEnabled ? "ft10.action.hud_shown" : "ft10.action.hud_hidden"));
        }
        while (undo.consumeClick()) {
            Optional<Rivalry> undone = ft10.session().map(Session::book).flatMap(ScoreBook::undo);
            HudCompat.actionBar(undone
                .<Component>map(r -> Texts.body("ft10.undo.done", Texts.value(r.name()), Texts.score(r.me(), r.them(), c)))
                .orElseGet(() -> Texts.colored(Texts.ERROR, "ft10.error.nothing_to_undo")));
        }
        while (reset.consumeClick()) {
            Optional<ScoreBook> book = ft10.session().map(Session::book);
            Optional<Rivalry> active = book.flatMap(ScoreBook::active);
            if (active.isPresent()) {
                book.get().reset(active.get().key());
                HudCompat.actionBar(Texts.body("ft10.reset.one", Texts.value(active.get().name())));
            } else {
                HudCompat.actionBar(Texts.colored(Texts.ERROR, "ft10.error.no_opponent"));
            }
        }
        while (settings.consumeClick()) {
            ClientCompat.setScreen(minecraft, ConfigScreen.create(null));
        }
        while (editHud.consumeClick()) {
            ClientCompat.setScreen(minecraft, new HudEditorScreen(null));
        }
    }
}

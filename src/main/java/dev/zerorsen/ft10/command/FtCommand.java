package dev.zerorsen.ft10.command;

import static dev.zerorsen.ft10.compat.CommandCompat.argument;
import static dev.zerorsen.ft10.compat.CommandCompat.literal;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import dev.zerorsen.ft10.Ft10;
import dev.zerorsen.ft10.config.ConfigScreen;
import dev.zerorsen.ft10.config.Ft10Config;
import dev.zerorsen.ft10.core.PlayerRef;
import dev.zerorsen.ft10.core.Rivalry;
import dev.zerorsen.ft10.core.ScoreBook;
import dev.zerorsen.ft10.core.Session;
import dev.zerorsen.ft10.feedback.Debug;
import dev.zerorsen.ft10.feedback.HudEditorScreen;
import dev.zerorsen.ft10.feedback.Texts;
import dev.zerorsen.ft10.mc.Players;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * {@code /ft10} (and the short {@code /ft} unless turned off). Client-side only: nothing here reaches the server.
 */
public final class FtCommand {
    private static final int OK = 1;
    private static final int LIST_LIMIT = 10;

    private FtCommand() {}

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(tree("ft10"));
        if (Ft10.get().config().shortAlias) dispatcher.register(tree("ft"));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> tree(String root) {
        return literal(root)
            .executes(FtCommand::status)
            .then(literal("help").executes(ctx -> help(ctx, root)))
            .then(literal("on").executes(ctx -> setEnabled(ctx, true)))
            .then(literal("off").executes(ctx -> setEnabled(ctx, false)))
            .then(literal("toggle").executes(ctx -> setEnabled(ctx, !Ft10.get().config().enabled)))
            // "all" is a value of the player argument (not a literal) so Brigadier sees no ambiguity.
            .then(literal("reset")
                .executes(FtCommand::resetActive)
                .then(argument("player", StringArgumentType.word())
                    .suggests(RESET_TARGETS)
                    .executes(ctx -> resetTarget(ctx, root, StringArgumentType.getString(ctx, "player"), false))
                    .then(literal("confirm")
                        .executes(ctx -> resetTarget(ctx, root, StringArgumentType.getString(ctx, "player"), true)))))
            .then(literal("set")
                .then(argument("me", IntegerArgumentType.integer(0, 999))
                    .then(argument("them", IntegerArgumentType.integer(0, 999))
                        .executes(FtCommand::set))))
            .then(literal("undo").executes(FtCommand::undo))
            .then(literal("goal")
                .executes(FtCommand::showGoal)
                .then(literal("off").executes(ctx -> setGoal(ctx, 0)))
                .then(argument("target", IntegerArgumentType.integer(1, 999))
                    .executes(ctx -> setGoal(ctx, IntegerArgumentType.getInteger(ctx, "target")))))
            .then(literal("lock")
                .then(argument("player", StringArgumentType.word())
                    .suggests(KNOWN_PLAYERS)
                    .executes(ctx -> lock(ctx, StringArgumentType.getString(ctx, "player")))))
            .then(literal("unlock").executes(FtCommand::unlock))
            .then(literal("list").executes(FtCommand::list))
            .then(literal("hud").executes(FtCommand::hud))
            .then(literal("config").executes(FtCommand::config))
            .then(literal("debug")
                .executes(FtCommand::toggleDebug)
                .then(literal("last").executes(FtCommand::debugLast)));
    }

    /** Online players plus everyone we have a score with on this server. */
    private static final SuggestionProvider<FabricClientCommandSource> KNOWN_PLAYERS = (ctx, builder) -> {
        Set<String> names = new LinkedHashSet<>(Players.onlineNames());
        Ft10.get().session().ifPresent(s -> s.book().byLastFought().forEach(r -> names.add(r.name())));
        return SharedSuggestionProvider.suggest(names, builder);
    };

    /** Like {@link #KNOWN_PLAYERS}, plus "all". */
    private static final SuggestionProvider<FabricClientCommandSource> RESET_TARGETS = (ctx, builder) -> {
        Set<String> names = new LinkedHashSet<>(List.of("all"));
        names.addAll(Players.onlineNames());
        Ft10.get().session().ifPresent(s -> s.book().byLastFought().forEach(r -> names.add(r.name())));
        return SharedSuggestionProvider.suggest(names, builder);
    };

    // ---- handlers -------------------------------------------------------------------------------------------

    private static int status(CommandContext<FabricClientCommandSource> ctx) {
        Ft10Config c = Ft10.get().config();
        Optional<Session> session = Ft10.get().session();
        Optional<Rivalry> active = session.flatMap(s -> s.book().active());
        MutableComponent state = c.enabled
            ? Texts.colored(Texts.SUCCESS, "ft10.state.enabled")
            : Texts.colored(Texts.ERROR, "ft10.state.disabled");

        if (active.isPresent()) {
            Rivalry r = active.get();
            MutableComponent line = Texts.body("ft10.status.vs", Texts.value(r.name()), Texts.score(r.me(), r.them(), c));
            if (c.goal > 0) line.append(" ").append(Texts.aside(Texts.literal("FT" + c.goal, Texts.BRAND)));
            if (r.hasSets()) {
                line.append(Texts.literal(" | ", Texts.FRAME)).append(Texts.body("ft10.sets", Texts.score(r.setsMe(), r.setsThem(), c)));
            }
            if (session.get().locked().isPresent()) line.append(" ").append(Texts.colored(Texts.NOTICE, "ft10.status.locked"));
            feedback(ctx, line);
        } else {
            feedback(ctx, Texts.body("ft10.status.none"));
        }
        feedback(ctx, state.append(Texts.literal(" — ", Texts.FRAME)).append(Texts.body("ft10.status.help_hint", Texts.value("/ft10 help"))));
        return OK;
    }

    private static int help(CommandContext<FabricClientCommandSource> ctx, String root) {
        ctx.getSource().sendFeedback(Texts.line(Texts.body("ft10.help.header")));
        helpLine(ctx, root, "", "ft10.help.status");
        helpLine(ctx, root, " on | off | toggle", "ft10.help.toggle");
        helpLine(ctx, root, " reset [player | all]", "ft10.help.reset");
        helpLine(ctx, root, " set <me> <them>", "ft10.help.set");
        helpLine(ctx, root, " undo", "ft10.help.undo");
        helpLine(ctx, root, " goal <n | off>", "ft10.help.goal");
        helpLine(ctx, root, " lock <player> | unlock", "ft10.help.lock");
        helpLine(ctx, root, " list", "ft10.help.list");
        helpLine(ctx, root, " hud", "ft10.help.hud");
        helpLine(ctx, root, " config", "ft10.help.config");
        helpLine(ctx, root, " debug [last]", "ft10.help.debug");
        return OK;
    }

    private static void helpLine(CommandContext<FabricClientCommandSource> ctx, String root, String args, String key) {
        ctx.getSource().sendFeedback(Texts.literal("/" + root + args, Texts.NOTICE)
            .append(Texts.literal(" — ", Texts.FRAME))
            .append(Texts.body(key)));
    }

    private static int setEnabled(CommandContext<FabricClientCommandSource> ctx, boolean enabled) {
        Ft10.get().config().enabled = enabled;
        Ft10.get().saveConfig();
        feedback(ctx, enabled
            ? Texts.colored(Texts.SUCCESS, "ft10.state.enabled")
            : Texts.colored(Texts.ERROR, "ft10.state.disabled"));
        return OK;
    }

    private static int resetActive(CommandContext<FabricClientCommandSource> ctx) {
        Optional<ScoreBook> book = book(ctx);
        if (book.isEmpty()) return 0;
        Optional<Rivalry> active = book.get().active();
        if (active.isEmpty()) return error(ctx, "ft10.error.no_opponent");
        book.get().reset(active.get().key());
        feedback(ctx, Texts.body("ft10.reset.one", Texts.value(active.get().name())));
        return OK;
    }

    private static int resetTarget(CommandContext<FabricClientCommandSource> ctx, String root, String target,
                                   boolean confirmed) {
        if (!target.equalsIgnoreCase("all")) return resetPlayer(ctx, target);
        return confirmed ? resetAll(ctx) : askResetAll(ctx, root);
    }

    private static int resetPlayer(CommandContext<FabricClientCommandSource> ctx, String name) {
        Optional<ScoreBook> book = book(ctx);
        if (book.isEmpty()) return 0;
        Optional<Rivalry> rivalry = findRivalry(book.get(), name);
        if (rivalry.isEmpty()) return error(ctx, "ft10.error.no_score", name);
        book.get().reset(rivalry.get().key());
        feedback(ctx, Texts.body("ft10.reset.one", Texts.value(rivalry.get().name())));
        return OK;
    }

    private static int askResetAll(CommandContext<FabricClientCommandSource> ctx, String root) {
        feedback(ctx, Texts.colored(Texts.ERROR, "ft10.reset.all.confirm")
            .append(" ")
            .append(Texts.button("ft10.reset.all.button", Texts.ERROR, "/" + root + " reset all confirm",
                "ft10.reset.all.button.hover")));
        return OK;
    }

    private static int resetAll(CommandContext<FabricClientCommandSource> ctx) {
        Optional<ScoreBook> book = book(ctx);
        if (book.isEmpty()) return 0;
        int count = book.get().resetAll();
        feedback(ctx, Texts.colored(Texts.SUCCESS, "ft10.reset.all.done", Texts.value(count)));
        return OK;
    }

    private static int set(CommandContext<FabricClientCommandSource> ctx) {
        Optional<Session> session = Ft10.get().session();
        if (session.isEmpty()) return error(ctx, "ft10.error.not_connected");
        Optional<Rivalry> active = session.get().book().active();
        if (active.isEmpty()) return error(ctx, "ft10.error.no_opponent");
        int me = IntegerArgumentType.getInteger(ctx, "me");
        int them = IntegerArgumentType.getInteger(ctx, "them");
        Rivalry r = session.get().book().set(active.get().ref(), me, them, System.currentTimeMillis());
        feedback(ctx, Texts.body("ft10.set.done", Texts.value(r.name()), Texts.score(r.me(), r.them(), Ft10.get().config())));
        return OK;
    }

    private static int undo(CommandContext<FabricClientCommandSource> ctx) {
        Optional<ScoreBook> book = book(ctx);
        if (book.isEmpty()) return 0;
        Optional<Rivalry> undone = book.get().undo();
        if (undone.isEmpty()) return error(ctx, "ft10.error.nothing_to_undo");
        Rivalry r = undone.get();
        feedback(ctx, Texts.body("ft10.undo.done", Texts.value(r.name()), Texts.score(r.me(), r.them(), Ft10.get().config())));
        return OK;
    }

    private static int showGoal(CommandContext<FabricClientCommandSource> ctx) {
        int goal = Ft10.get().config().goal;
        feedback(ctx, goal > 0
            ? Texts.body("ft10.goal.current", Texts.literal("FT" + goal, Texts.BRAND))
            : Texts.body("ft10.goal.none"));
        return OK;
    }

    private static int setGoal(CommandContext<FabricClientCommandSource> ctx, int goal) {
        Ft10.get().config().goal = goal;
        Ft10.get().saveConfig();
        feedback(ctx, goal > 0
            ? Texts.colored(Texts.SUCCESS, "ft10.goal.set", Texts.literal("FT" + goal, Texts.BRAND))
            : Texts.body("ft10.goal.off"));
        return OK;
    }

    private static int lock(CommandContext<FabricClientCommandSource> ctx, String name) {
        Optional<Session> session = Ft10.get().session();
        if (session.isEmpty()) return error(ctx, "ft10.error.not_connected");
        Optional<PlayerRef> player = Players.byName(name)
            .or(() -> findRivalry(session.get().book(), name).map(Rivalry::ref));
        if (player.isEmpty()) return error(ctx, "ft10.error.unknown_player", name);
        session.get().lock(player.get());
        feedback(ctx, Texts.body("ft10.lock.done", Texts.value(player.get().name())));
        return OK;
    }

    private static int unlock(CommandContext<FabricClientCommandSource> ctx) {
        Ft10.get().session().ifPresent(Session::unlock);
        feedback(ctx, Texts.body("ft10.lock.released"));
        return OK;
    }

    private static int list(CommandContext<FabricClientCommandSource> ctx) {
        Optional<ScoreBook> book = book(ctx);
        if (book.isEmpty()) return 0;
        List<Rivalry> rivals = book.get().byLastFought();
        if (rivals.isEmpty()) {
            feedback(ctx, Texts.body("ft10.list.empty"));
            return OK;
        }
        Ft10Config c = Ft10.get().config();
        feedback(ctx, Texts.body("ft10.list.header", Texts.value(rivals.size())));
        for (Rivalry r : rivals.subList(0, Math.min(LIST_LIMIT, rivals.size()))) {
            MutableComponent line = Texts.literal("• ", Texts.FRAME)
                .append(Texts.value(r.name()))
                .append(Texts.literal(" — ", Texts.FRAME))
                .append(Texts.score(r.me(), r.them(), c));
            if (r.hasSets()) {
                line.append(" ").append(Texts.aside(Texts.body("ft10.sets", Texts.score(r.setsMe(), r.setsThem(), c))));
            }
            line.append(" ").append(Texts.button("ft10.list.reset", Texts.ERROR, "/ft10 reset " + r.name(),
                "ft10.list.reset.hover"));
            ctx.getSource().sendFeedback(line);
        }
        return OK;
    }

    private static int config(CommandContext<FabricClientCommandSource> ctx) {
        Ft10.get().openScreenNextTick(ConfigScreen.create(null));
        return OK;
    }

    private static int hud(CommandContext<FabricClientCommandSource> ctx) {
        Ft10.get().openScreenNextTick(new HudEditorScreen(null));
        return OK;
    }

    private static int toggleDebug(CommandContext<FabricClientCommandSource> ctx) {
        Debug debug = Ft10.get().debug();
        debug.setEnabled(!debug.enabled());
        feedback(ctx, debug.enabled()
            ? Texts.colored(Texts.SUCCESS, "ft10.debug.on")
            : Texts.body("ft10.debug.off"));
        return OK;
    }

    private static int debugLast(CommandContext<FabricClientCommandSource> ctx) {
        List<Debug.Unrecognized> recent = Ft10.get().debug().recent();
        if (recent.isEmpty()) {
            feedback(ctx, Texts.body("ft10.debug.last.empty"));
            return OK;
        }
        feedback(ctx, Texts.body("ft10.debug.last.header", Texts.value(recent.size())));
        for (Debug.Unrecognized entry : recent) ctx.getSource().sendFeedback(Debug.describe(entry));
        return OK;
    }

    // ---- helpers --------------------------------------------------------------------------------------------

    private static Optional<ScoreBook> book(CommandContext<FabricClientCommandSource> ctx) {
        Optional<ScoreBook> book = Ft10.get().session().map(Session::book);
        if (book.isEmpty()) error(ctx, "ft10.error.not_connected");
        return book;
    }

    private static Optional<Rivalry> findRivalry(ScoreBook book, String name) {
        return Players.byName(name).flatMap(p -> book.find(p.key())).or(() -> book.findByName(name));
    }

    private static void feedback(CommandContext<FabricClientCommandSource> ctx, Component message) {
        ctx.getSource().sendFeedback(Texts.line(message));
    }

    private static int error(CommandContext<FabricClientCommandSource> ctx, String key, Object... args) {
        Object[] values = new Object[args.length];
        for (int i = 0; i < args.length; i++) values[i] = Texts.value(args[i]);
        ctx.getSource().sendFeedback(Texts.line(Texts.colored(Texts.ERROR, key, values)));
        return 0;
    }
}

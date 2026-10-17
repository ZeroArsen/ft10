package dev.zerorsen.ft10.mc;

import dev.zerorsen.ft10.compat.TextCompat;
import dev.zerorsen.ft10.core.DeathMessageParser;
import dev.zerorsen.ft10.core.PlayerRef;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Reads death messages. Vanilla and Paper send them as translatable {@code death.*} components whose arguments
 * are the victim and killer names (with a show_entity hover carrying the UUID), often wrapped in a plugin's
 * prefix. Kill-message plugins send plain text, which goes through the regex {@link DeathMessageParser}.
 */
public final class DeathMessages {
    /** Deeper than any real chat component; stops a hostile, deeply nested message. */
    private static final int MAX_DEPTH = 8;

    /**
     * @param killer      null when the message names no player killer
     * @param killerKnown false when the message leaves the killer open (then the hit history decides)
     * @param source      "vanilla" or the matching pattern's label, for debug output
     */
    public record Death(PlayerRef victim, PlayerRef killer, boolean killerKnown, String source) {}

    /**
     * What the parser may use besides the tab list.
     *
     * @param self           the player themself
     * @param recentOpponent someone we just fought, by name: lets a kill count even when that player already left
     *                       the tab list or the server hides it
     * @param fighting       whether we just fought this player; broad patterns only count against them
     */
    public record Context(PlayerRef self, Function<String, Optional<PlayerRef>> recentOpponent,
                          Function<PlayerRef, Boolean> fighting) {
        public static Context of(PlayerRef self) {
            return new Context(self, name -> Optional.empty(), player -> false);
        }
    }

    private DeathMessages() {}

    public static Optional<Death> parse(Component message, DeathMessageParser parser, Context context) {
        Optional<TranslatableContents> vanilla = findDeath(message, 0);
        if (vanilla.isPresent()) return fromTranslatable(vanilla.get(), context);
        return parser.parse(message.getString()).flatMap(parsed -> fromParsed(parsed, message, context));
    }

    /** The first {@code death.*} translatable anywhere in the message: "☠ " + death message, prefixes, … */
    private static Optional<TranslatableContents> findDeath(Component component, int depth) {
        if (depth > MAX_DEPTH) return Optional.empty();
        if (component.getContents() instanceof TranslatableContents translatable) {
            if (translatable.getKey().startsWith("death.")) return Optional.of(translatable);
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component child) {
                    Optional<TranslatableContents> found = findDeath(child, depth + 1);
                    if (found.isPresent()) return found;
                }
            }
        }
        for (Component sibling : component.getSiblings()) {
            Optional<TranslatableContents> found = findDeath(sibling, depth + 1);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    private static Optional<Death> fromTranslatable(TranslatableContents contents, Context context) {
        Object[] args = contents.getArgs();
        if (args.length == 0) return Optional.empty();
        Optional<PlayerRef> victim = asPlayer(args[0], context);
        if (victim.isEmpty()) return Optional.empty(); // a named mob or pet died
        PlayerRef killer = null;
        for (int i = 1; i < args.length && killer == null; i++) {
            killer = asPlayer(args[i], context).orElse(null);
        }
        return Optional.of(new Death(victim.get(), killer, true, "vanilla"));
    }

    private static Optional<PlayerRef> asPlayer(Object arg, Context context) {
        if (arg instanceof Component component) {
            Optional<PlayerRef> player = Players.fromComponent(component);
            if (player.isPresent() || TextCompat.hoverEntity(component.getStyle()).isPresent()) return player;
            return self(component.getString(), context);
        }
        if (arg instanceof String name) return Players.byName(name).or(() -> self(name, context));
        return Optional.empty();
    }

    private static Optional<Death> fromParsed(DeathMessageParser.Parsed parsed, Component message, Context context) {
        List<TextCompat.HoverEntity> hovers = hoveredPlayers(message);
        PlayerRef self = context.self();
        PlayerRef victim = resolve(parsed.victim(), hovers, context).orElse(null);
        PlayerRef killer = parsed.killer() == null ? null : resolve(parsed.killer(), hovers, context).orElse(null);

        // A name we can't find on the tab list still counts when the other side is us and it's someone we fought.
        if (victim == null && self.sameAs(killer)) {
            victim = context.recentOpponent().apply(parsed.victim()).orElse(null);
        }
        if (killer == null && parsed.killer() != null && self.sameAs(victim)) {
            killer = context.recentOpponent().apply(parsed.killer()).orElse(null);
        }
        if (victim == null) return Optional.empty();

        if (parsed.loose()) {
            // "X was <anything> by Y" is only a kill when it's about us and someone we were just fighting.
            PlayerRef other = self.sameAs(victim) ? killer : self.sameAs(killer) ? victim : null;
            if (other == null || !context.fighting().apply(other)) return Optional.empty();
        }
        // A named killer we couldn't find is someone else, not "unknown": don't fall back to our hits.
        boolean killerKnown = parsed.killer() != null;
        return Optional.of(new Death(victim, killer, killerKnown, parsed.label()));
    }

    private static Optional<PlayerRef> resolve(String name, List<TextCompat.HoverEntity> hovers, Context context) {
        if (DeathMessageParser.SELF.equals(name)) return Optional.of(context.self());
        for (TextCompat.HoverEntity hover : hovers) {
            if (hover.name() != null && Players.mentions(hover.name(), name)) {
                return Players.byUuid(hover.uuid()).or(() -> Optional.of(PlayerRef.of(hover.uuid(), name)));
            }
        }
        return Players.byName(name)
            .or(() -> self(name, context))
            .or(() -> Players.byDisplayName(name));
    }

    private static Optional<PlayerRef> self(String name, Context context) {
        return Players.mentions(name, context.self().name())
            ? Optional.of(context.self())
            : Optional.empty();
    }

    /** Every player show_entity hover in the message, in order. */
    private static List<TextCompat.HoverEntity> hoveredPlayers(Component message) {
        List<TextCompat.HoverEntity> found = new ArrayList<>();
        collectHovers(message, found, 0);
        return found;
    }

    private static void collectHovers(Component component, List<TextCompat.HoverEntity> found, int depth) {
        if (depth > MAX_DEPTH) return;
        TextCompat.hoverEntity(component.getStyle())
            .filter(TextCompat.HoverEntity::isPlayer)
            .map(h -> h.name() != null ? h : new TextCompat.HoverEntity(h.uuid(), true, component.getString()))
            .ifPresent(found::add);
        if (component.getContents() instanceof TranslatableContents translatable) {
            for (Object arg : translatable.getArgs()) {
                if (arg instanceof Component child) collectHovers(child, found, depth + 1);
            }
        }
        for (Component sibling : component.getSiblings()) collectHovers(sibling, found, depth + 1);
    }
}

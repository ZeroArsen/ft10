package dev.zerorsen.ft10.mc;

import dev.zerorsen.ft10.core.DeathMessageParser;
import dev.zerorsen.ft10.core.PlayerRef;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * Reads death messages. Vanilla and Paper send them as translatable {@code death.*} components whose arguments
 * are the victim and killer names (with a show_entity hover carrying the UUID); servers with kill-message
 * plugins send plain text, which goes through the regex {@link DeathMessageParser}.
 */
public final class DeathMessages {
    /** @param killer null when the message names no player killer */
    public record Death(PlayerRef victim, PlayerRef killer) {}

    private DeathMessages() {}

    public static Optional<Death> parse(Component message, DeathMessageParser parser) {
        if (message.getContents() instanceof TranslatableContents translatable
            && translatable.getKey().startsWith("death.")) {
            return fromTranslatable(translatable);
        }
        return parser.parse(message.getString()).flatMap(parsed -> {
            Optional<PlayerRef> victim = Players.byName(parsed.victim());
            if (victim.isEmpty()) return Optional.empty();
            PlayerRef killer = parsed.killer() == null ? null : Players.byName(parsed.killer()).orElse(null);
            return Optional.of(new Death(victim.get(), killer));
        });
    }

    private static Optional<Death> fromTranslatable(TranslatableContents contents) {
        Object[] args = contents.getArgs();
        if (args.length == 0) return Optional.empty();
        Optional<PlayerRef> victim = asPlayer(args[0]);
        if (victim.isEmpty()) return Optional.empty(); // a named mob or pet died
        PlayerRef killer = null;
        for (int i = 1; i < args.length && killer == null; i++) {
            killer = asPlayer(args[i]).orElse(null);
        }
        return Optional.of(new Death(victim.get(), killer));
    }

    private static Optional<PlayerRef> asPlayer(Object arg) {
        if (arg instanceof Component component) return Players.fromComponent(component);
        if (arg instanceof String name) return Players.byName(name);
        return Optional.empty();
    }
}

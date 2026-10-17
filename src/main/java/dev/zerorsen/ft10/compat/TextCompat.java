package dev.zerorsen.ft10.compat;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
//? if >=26.1 {
import net.minecraft.world.entity.EntityTypes;
//?} else {
/*import net.minecraft.world.entity.EntityType;
*///?}

/**
 * Hover and click events became records with one subclass per action in 1.21.5. The entity type constants moved
 * from {@code EntityType} to {@code EntityTypes} in 26.x.
 */
public final class TextCompat {
    private TextCompat() {}

    /** What a name's show_entity hover says about the entity behind it. */
    public record HoverEntity(UUID uuid, boolean isPlayer, String name) {}

    public static Optional<HoverEntity> hoverEntity(Style style) {
        if (!(style.getHoverEvent() instanceof HoverEvent.ShowEntity show)) return Optional.empty();
        HoverEvent.EntityTooltipInfo info = show.entity();
        String name = info.name.map(Component::getString).orElse(null);
        //? if >=26.1 {
        boolean isPlayer = info.type == EntityTypes.PLAYER;
        //?} else {
        /*boolean isPlayer = info.type == EntityType.PLAYER;
        *///?}
        return Optional.of(new HoverEntity(info.uuid, isPlayer, name));
    }

    public static Style runCommand(Style style, String command) {
        return style.withClickEvent(new ClickEvent.RunCommand(command));
    }

    public static Style hoverText(Style style, Component text) {
        return style.withHoverEvent(new HoverEvent.ShowText(text));
    }
}

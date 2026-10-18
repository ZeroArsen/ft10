package dev.zerorsen.ft10.compat;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
//? if >=26.2 {
import net.minecraft.world.entity.EntityTypes;
//?} else {
/*import net.minecraft.world.entity.EntityType;
*///?}

/**
 * Hover and click events became records with one subclass per action in 1.21.5. The entity type constants moved
 * from {@code EntityType} to {@code EntityTypes} in 26.2.
 */
public final class TextCompat {
    private TextCompat() {}

    /** What a name's show_entity hover says about the entity behind it. */
    public record HoverEntity(UUID uuid, boolean isPlayer, String name) {}

    public static Optional<HoverEntity> hoverEntity(Style style) {
        //? if >=1.21.5 {
        if (!(style.getHoverEvent() instanceof HoverEvent.ShowEntity show)) return Optional.empty();
        HoverEvent.EntityTooltipInfo info = show.entity();
        UUID uuid = info.uuid;
        //?} else {
        /*HoverEvent hover = style.getHoverEvent();
        HoverEvent.EntityTooltipInfo info = hover == null ? null : hover.getValue(HoverEvent.Action.SHOW_ENTITY);
        if (info == null) return Optional.empty();
        UUID uuid = info.id;
        *///?}
        String name = info.name.map(Component::getString).orElse(null);
        //? if >=26.2 {
        boolean isPlayer = info.type == EntityTypes.PLAYER;
        //?} else {
        /*boolean isPlayer = info.type == EntityType.PLAYER;
        *///?}
        return Optional.of(new HoverEntity(uuid, isPlayer, name));
    }

    public static Style runCommand(Style style, String command) {
        //? if >=1.21.5 {
        return style.withClickEvent(new ClickEvent.RunCommand(command));
        //?} else {
        /*return style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        *///?}
    }

    /** Clicking copies {@code text} to the clipboard (local, nothing is sent). */
    public static Style copyToClipboard(Style style, String text) {
        //? if >=1.21.5 {
        return style.withClickEvent(new ClickEvent.CopyToClipboard(text));
        //?} else {
        /*return style.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text));
        *///?}
    }

    public static Style hoverText(Style style, Component text) {
        //? if >=1.21.5 {
        return style.withHoverEvent(new HoverEvent.ShowText(text));
        //?} else {
        /*return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, text));
        *///?}
    }
}

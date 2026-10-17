package dev.zerorsen.ft10.mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.zerorsen.ft10.core.DeathMessageParser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
//? if >=26.1 {
import net.minecraft.world.entity.EntityTypes;
//?}
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Reads death messages built the way the server builds them (names carry a show_entity hover). */
class DeathMessagesTest {
    private static final UUID STEVE = UUID.randomUUID();
    private static final UUID ALEX = UUID.randomUUID();
    private static final DeathMessageParser NO_PATTERNS = new DeathMessageParser(false, List.of());

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void playerKilledByPlayer() {
        Component message = Component.translatable("death.attack.player",
            name(EntityTypes.PLAYER, STEVE, "Steve"), name(EntityTypes.PLAYER, ALEX, "Alex"));

        DeathMessages.Death death = DeathMessages.parse(message, NO_PATTERNS).orElseThrow();
        assertEquals(STEVE, death.victim().uuid());
        assertEquals(ALEX, death.killer().uuid());
        assertEquals("Alex", death.killer().name());
    }

    @Test
    void killerIsFoundAfterTheWeaponArgument() {
        Component weapon = Component.literal("[Sharp Sword]");
        Component message = Component.translatable("death.attack.player.item",
            name(EntityTypes.PLAYER, STEVE, "Steve"), name(EntityTypes.PLAYER, ALEX, "Alex"), weapon);

        assertEquals(ALEX, DeathMessages.parse(message, NO_PATTERNS).orElseThrow().killer().uuid());
    }

    @Test
    void mobKillerIsNotAPlayer() {
        Component message = Component.translatable("death.attack.mob",
            name(EntityTypes.PLAYER, STEVE, "Steve"), name(EntityTypes.ZOMBIE, UUID.randomUUID(), "Zombie"));

        DeathMessages.Death death = DeathMessages.parse(message, NO_PATTERNS).orElseThrow();
        assertEquals(STEVE, death.victim().uuid());
        assertNull(death.killer());
    }

    @Test
    void deadPetIsIgnored() {
        Component message = Component.translatable("death.attack.player",
            name(EntityTypes.WOLF, UUID.randomUUID(), "Rex"), name(EntityTypes.PLAYER, ALEX, "Alex"));

        assertTrue(DeathMessages.parse(message, NO_PATTERNS).isEmpty());
    }

    @Test
    void otherTranslatableMessagesAreIgnored() {
        Component message = Component.translatable("multiplayer.player.joined", name(EntityTypes.PLAYER, ALEX, "Alex"));
        assertTrue(DeathMessages.parse(message, NO_PATTERNS).isEmpty());
    }

    @Test
    void plainTextNeedsTheNamesOnTheTabList() {
        // Without a connection there is no tab list, so even a matching plugin message can't be tied to players.
        DeathMessageParser presets = new DeathMessageParser(true, List.of());
        Optional<DeathMessages.Death> death = DeathMessages.parse(Component.literal("Steve was slain by Alex"), presets);
        assertTrue(death.isEmpty());
    }

    private static MutableComponent name(EntityType<?> type, UUID uuid, String name) {
        return Component.literal(name).withStyle(style -> style.withHoverEvent(
            new HoverEvent.ShowEntity(new HoverEvent.EntityTooltipInfo(type, uuid, Component.literal(name)))));
    }

    // Before 26.x the constants were on EntityType itself. A nested class loads lazily, after the bootstrap.
    //? if <26.1 {
    /*private static final class EntityTypes {
        static final EntityType<?> PLAYER = EntityType.PLAYER;
        static final EntityType<?> ZOMBIE = EntityType.ZOMBIE;
        static final EntityType<?> WOLF = EntityType.WOLF;
    }
    *///?}
}

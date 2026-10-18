package dev.zerorsen.ft10.mc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.zerorsen.ft10.core.DeathMessageParser;
import dev.zerorsen.ft10.core.PlayerRef;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.EntityType;
//? if >=26.2 {
import net.minecraft.world.entity.EntityTypes;
//?}
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Reads death messages built the way the server builds them (names carry a show_entity hover). */
class DeathMessagesTest {
    private static final UUID STEVE = UUID.randomUUID();
    private static final UUID ALEX = UUID.randomUUID();
    private static final UUID ME_ID = UUID.randomUUID();
    private static final PlayerRef ME = PlayerRef.of(ME_ID, "Me");
    private static final DeathMessageParser NO_PATTERNS = new DeathMessageParser(false, List.of());
    private static final DeathMessageParser PRESETS = new DeathMessageParser(true, List.of());
    private static final DeathMessages.Context CONTEXT = DeathMessages.Context.of(ME);

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void playerKilledByPlayer() {
        Component message = Component.translatable("death.attack.player",
            name(EntityTypes.PLAYER, STEVE, "Steve"), name(EntityTypes.PLAYER, ALEX, "Alex"));

        DeathMessages.Death death = DeathMessages.parse(message, NO_PATTERNS, CONTEXT).orElseThrow();
        assertEquals(STEVE, death.victim().uuid());
        assertEquals(ALEX, death.killer().uuid());
        assertEquals("Alex", death.killer().name());
    }

    @Test
    void killerIsFoundAfterTheWeaponArgument() {
        Component weapon = Component.literal("[Sharp Sword]");
        Component message = Component.translatable("death.attack.player.item",
            name(EntityTypes.PLAYER, STEVE, "Steve"), name(EntityTypes.PLAYER, ALEX, "Alex"), weapon);

        assertEquals(ALEX, DeathMessages.parse(message, NO_PATTERNS, CONTEXT).orElseThrow().killer().uuid());
    }

    @Test
    void mobKillerIsNotAPlayer() {
        Component message = Component.translatable("death.attack.mob",
            name(EntityTypes.PLAYER, STEVE, "Steve"), name(EntityTypes.ZOMBIE, UUID.randomUUID(), "Zombie"));

        DeathMessages.Death death = DeathMessages.parse(message, NO_PATTERNS, CONTEXT).orElseThrow();
        assertEquals(STEVE, death.victim().uuid());
        assertNull(death.killer());
    }

    @Test
    void deadPetIsIgnored() {
        Component message = Component.translatable("death.attack.player",
            name(EntityTypes.WOLF, UUID.randomUUID(), "Rex"), name(EntityTypes.PLAYER, ALEX, "Alex"));

        assertTrue(DeathMessages.parse(message, NO_PATTERNS, CONTEXT).isEmpty());
    }

    @Test
    void otherTranslatableMessagesAreIgnored() {
        Component message = Component.translatable("multiplayer.player.joined", name(EntityTypes.PLAYER, ALEX, "Alex"));
        assertTrue(DeathMessages.parse(message, NO_PATTERNS, CONTEXT).isEmpty());
    }

    @Test
    void plainTextNeedsTheNamesOnTheTabList() {
        // Without a connection there is no tab list, so even a matching plugin message can't be tied to players.
        DeathMessageParser presets = new DeathMessageParser(true, List.of());
        Optional<DeathMessages.Death> death = DeathMessages.parse(Component.literal("Steve was slain by Alex"), presets, CONTEXT);
        assertTrue(death.isEmpty());
    }

    @Test
    void wrappedVanillaMessageIsFound() {
        // Paper plugins often prefix the vanilla message: "☠ " + translatable, inside another component.
        Component vanilla = Component.translatable("death.attack.player",
            name(EntityTypes.PLAYER, STEVE, "Steve"), name(EntityTypes.PLAYER, ALEX, "Alex"));
        Component message = Component.literal("").append(Component.literal("☠ ")).append(vanilla);

        DeathMessages.Death death = DeathMessages.parse(message, NO_PATTERNS, CONTEXT).orElseThrow();
        assertEquals(STEVE, death.victim().uuid());
        assertEquals(ALEX, death.killer().uuid());
        assertEquals("vanilla", death.source());
    }

    @Test
    void pluginMessageNamesAreReadFromHovers() {
        // No tab list in tests: the hover's UUID alone ties the plain-text names to players.
        Component message = Component.literal("")
            .append(name(EntityTypes.PLAYER, STEVE, "Steve"))
            .append(Component.literal(" was slain by "))
            .append(name(EntityTypes.PLAYER, ALEX, "Alex"));

        DeathMessages.Death death = DeathMessages.parse(message, PRESETS, CONTEXT).orElseThrow();
        assertEquals(STEVE, death.victim().uuid());
        assertEquals(ALEX, death.killer().uuid());
        assertTrue(death.killerKnown());
        assertEquals("was-killed-by", death.source());
    }

    @Test
    void secondPersonKillUsesARecentOpponentByName() {
        PlayerRef steve = PlayerRef.of(STEVE, "Steve");
        DeathMessages.Context fought = new DeathMessages.Context(ME,
            n -> n.equalsIgnoreCase("Steve") ? Optional.of(steve) : Optional.empty(), p -> p.sameAs(steve));

        DeathMessages.Death death = DeathMessages.parse(Component.literal("You killed Steve!"), PRESETS, fought)
            .orElseThrow();
        assertEquals(STEVE, death.victim().uuid());
        assertEquals(ME_ID, death.killer().uuid());

        // Someone we never fought and can't find: not a kill we can count.
        assertTrue(DeathMessages.parse(Component.literal("You killed Herobrine!"), PRESETS, fought).isEmpty());
    }

    @Test
    void ourOwnNameCountsAsUs() {
        PlayerRef alex = PlayerRef.of(ALEX, "Alex");
        DeathMessages.Context fought = new DeathMessages.Context(ME,
            n -> n.equalsIgnoreCase("Alex") ? Optional.of(alex) : Optional.empty(), p -> p.sameAs(alex));

        DeathMessages.Death death = DeathMessages.parse(Component.literal("Me was slain by Alex"), PRESETS, fought)
            .orElseThrow();
        assertEquals(ME_ID, death.victim().uuid());
        assertEquals(ALEX, death.killer().uuid());
    }

    @Test
    void loosePatternNeedsACurrentFight() {
        PlayerRef alex = PlayerRef.of(ALEX, "Alex");
        Component message = Component.literal("You were struck down by Alex");
        DeathMessages.Context notFighting = new DeathMessages.Context(ME, n -> Optional.of(alex), p -> false);
        DeathMessages.Context fighting = new DeathMessages.Context(ME, n -> Optional.of(alex), p -> p.sameAs(alex));

        assertTrue(DeathMessages.parse(message, PRESETS, notFighting).isEmpty());
        assertEquals(ALEX, DeathMessages.parse(message, PRESETS, fighting).orElseThrow().killer().uuid());
    }

    @Test
    void roundWinnerDecidesWhoDied() {
        PlayerRef steve = PlayerRef.of(STEVE, "Steve");
        DeathMessages.Context duel = new DeathMessages.Context(ME,
            n -> n.equalsIgnoreCase("Steve") ? Optional.of(steve) : Optional.empty(), p -> p.sameAs(steve),
            () -> Optional.of(steve));

        // We won: our opponent died to us.
        DeathMessages.Death won = DeathMessages.parse(Component.literal("Me won the round 1 - 0"), PRESETS, duel)
            .orElseThrow();
        assertEquals(STEVE, won.victim().uuid());
        assertEquals(ME_ID, won.killer().uuid());

        // The opponent won: we died to them.
        DeathMessages.Death lost = DeathMessages.parse(Component.literal("Steve won the round 1 - 1"), PRESETS, duel)
            .orElseThrow();
        assertEquals(ME_ID, lost.victim().uuid());
        assertEquals(STEVE, lost.killer().uuid());

        // A duel we only watch: nothing to do with us.
        assertTrue(DeathMessages.parse(Component.literal("Herobrine won the round 3 - 0"), PRESETS, duel).isEmpty());
    }

    @Test
    void roundWonTitleNeedsAnOpponent() {
        PlayerRef steve = PlayerRef.of(STEVE, "Steve");
        DeathMessages.Context duel = new DeathMessages.Context(ME, n -> Optional.empty(), p -> false,
            () -> Optional.of(steve));
        assertEquals(STEVE, DeathMessages.parse(Component.literal("Round Won"), PRESETS, duel).orElseThrow()
            .victim().uuid());
        assertTrue(DeathMessages.parse(Component.literal("Round Won"), PRESETS, CONTEXT).isEmpty());
    }

    @Test
    void messageWithoutKillerLeavesItToTheHits() {
        DeathMessageParser parser = new DeathMessageParser(false, List.of("{victim} fell into the void"));
        Component message = Component.literal("")
            .append(name(EntityTypes.PLAYER, STEVE, "Steve"))
            .append(Component.literal(" fell into the void"));

        DeathMessages.Death death = DeathMessages.parse(message, parser, CONTEXT).orElseThrow();
        assertNull(death.killer());
        assertFalse(death.killerKnown());
    }

    private static MutableComponent name(EntityType<?> type, UUID uuid, String name) {
        var info = new HoverEvent.EntityTooltipInfo(type, uuid, Component.literal(name));
        //? if >=1.21.5 {
        HoverEvent hover = new HoverEvent.ShowEntity(info);
        //?} else {
        /*HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_ENTITY, info);
        *///?}
        return Component.literal(name).withStyle(style -> style.withHoverEvent(hover));
    }

    // Before 26.2 the constants were on EntityType itself. A nested class loads lazily, after the bootstrap.
    //? if <26.2 {
    /*private static final class EntityTypes {
        static final EntityType<?> PLAYER = EntityType.PLAYER;
        static final EntityType<?> ZOMBIE = EntityType.ZOMBIE;
        static final EntityType<?> WOLF = EntityType.WOLF;
    }
    *///?}
}

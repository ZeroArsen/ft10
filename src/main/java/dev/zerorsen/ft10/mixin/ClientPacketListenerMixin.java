package dev.zerorsen.ft10.mixin;

import dev.zerorsen.ft10.Ft10;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.world.entity.EntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Packets with no Fabric event. Both handlers first hop to the main thread (throwing on the network thread),
 * so injecting at RETURN means we only ever run on the main thread.
 */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
    @Inject(method = "handleDamageEvent", at = @At("RETURN"))
    private void ft10$onDamageEvent(ClientboundDamageEventPacket packet, CallbackInfo ci) {
        Ft10.get().onDamage(packet.entityId(), packet.sourceCauseId());
    }

    /**
     * The server broadcasts entity event 3 (death) the moment a player dies. With instant respawn this is the
     * only sign other clients get: the player is replaced before their health of 0 is ever synced.
     */
    @Inject(method = "handleEntityEvent", at = @At("RETURN"))
    private void ft10$onEntityEvent(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (packet.getEventId() == EntityEvent.DEATH) Ft10.get().onEntityDeath(packet);
    }

    @Inject(method = "handlePlayerCombatKill", at = @At("RETURN"))
    private void ft10$onPlayerCombatKill(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
        Ft10.get().onCombatKill(packet.playerId(), packet.message());
    }
}

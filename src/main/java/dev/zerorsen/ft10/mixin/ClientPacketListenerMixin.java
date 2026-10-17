package dev.zerorsen.ft10.mixin;

import dev.zerorsen.ft10.Ft10;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.world.entity.EntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Packets with no Fabric event. Every handler first hops to the main thread (throwing on the network thread),
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

    /** {@code /title … actionbar} and plugin action bars (system chat overlays come through Fabric's event). */
    @Inject(method = "setActionBarText", at = @At("RETURN"))
    private void ft10$onActionBar(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
        Ft10.get().onActionBar(packet.text());
    }

    @Inject(method = "setTitleText", at = @At("RETURN"))
    private void ft10$onTitle(ClientboundSetTitleTextPacket packet, CallbackInfo ci) {
        Ft10.get().onTitle(packet.text());
    }

    @Inject(method = "setSubtitleText", at = @At("RETURN"))
    private void ft10$onSubtitle(ClientboundSetSubtitleTextPacket packet, CallbackInfo ci) {
        Ft10.get().onSubtitle(packet.text());
    }

    @Inject(method = "handlePlayerInfoRemove", at = @At("RETURN"))
    private void ft10$onPlayerInfoRemove(ClientboundPlayerInfoRemovePacket packet, CallbackInfo ci) {
        Ft10.get().onPlayersLeft(packet.profileIds());
    }
}

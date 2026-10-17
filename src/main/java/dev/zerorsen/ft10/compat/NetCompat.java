package dev.zerorsen.ft10.compat;

import dev.zerorsen.ft10.net.Ft10Protocol;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * Registers the server-integration payloads. Fabric renamed {@code PayloadTypeRegistry.playS2C()} to
 * {@code clientboundPlay()} in 26.x. Registering the receivers also makes the client announce the channels to
 * the server, which is how a plugin knows the player has FT10.
 */
public final class NetCompat {
    private NetCompat() {}

    public static void register(Consumer<Ft10Protocol.Hello> onHello, Consumer<Ft10Protocol.Kill> onKill) {
        PayloadTypeRegistry<RegistryFriendlyByteBuf> clientbound = clientboundPlay();
        clientbound.register(Ft10Protocol.Hello.TYPE, Ft10Protocol.Hello.CODEC);
        clientbound.register(Ft10Protocol.Kill.TYPE, Ft10Protocol.Kill.CODEC);
        // Fabric runs play payload handlers on the client thread.
        ClientPlayNetworking.registerGlobalReceiver(Ft10Protocol.Hello.TYPE, (payload, context) -> onHello.accept(payload));
        ClientPlayNetworking.registerGlobalReceiver(Ft10Protocol.Kill.TYPE, (payload, context) -> onKill.accept(payload));
    }

    private static PayloadTypeRegistry<RegistryFriendlyByteBuf> clientboundPlay() {
        //? if >=26.1 {
        return PayloadTypeRegistry.clientboundPlay();
        //?} else {
        /*return PayloadTypeRegistry.playS2C();
        *///?}
    }
}

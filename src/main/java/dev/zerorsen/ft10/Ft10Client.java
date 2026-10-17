package dev.zerorsen.ft10;

import dev.zerorsen.ft10.command.FtCommand;
import dev.zerorsen.ft10.compat.HudCompat;
import dev.zerorsen.ft10.compat.NetCompat;
import dev.zerorsen.ft10.feedback.ScoreHud;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Ft10Client implements ClientModInitializer {
    public static final String MOD_ID = "ft10";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final String VERSION = /*$ mod_version*/ "0.1.0";
    public static final String MINECRAFT = /*$ minecraft*/ "26.2";

    @Override
    public void onInitializeClient() {
        FabricLoader loader = FabricLoader.getInstance();
        Ft10 ft10 = Ft10.create(loader.getConfigDir(), loader.getGameDir());

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ft10.onJoin(client));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ft10.onDisconnect());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ft10.onStopping());
        ClientTickEvents.END_CLIENT_TICK.register(ft10::onTick);
        ClientReceiveMessageEvents.GAME.register(ft10::onGameMessage);
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (level.isClientSide()) ft10.onSwing(entity);
            return InteractionResult.PASS;
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> FtCommand.register(dispatcher));
        HudCompat.register(Identifier.fromNamespaceAndPath(MOD_ID, "score"), ScoreHud::render);
        Keybinds.register();
        NetCompat.register(ft10::onServerHello, ft10::onServerKill);

        LOGGER.info("FT10 {} loaded for Minecraft {}", VERSION, MINECRAFT);
    }
}

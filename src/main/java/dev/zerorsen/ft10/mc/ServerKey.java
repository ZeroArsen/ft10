package dev.zerorsen.ft10.mc;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;

/** Which server we're on, so every server keeps its own scores. */
public final class ServerKey {
    private ServerKey() {}

    public static String current(Minecraft minecraft) {
        IntegratedServer integrated = minecraft.getSingleplayerServer();
        if (integrated != null) return "singleplayer:" + integrated.getWorldData().getLevelName();

        ServerData server = minecraft.getCurrentServer();
        if (server == null) return "unknown";
        if (server.isRealm()) return "realms:" + server.name;
        String address = server.ip.trim().toLowerCase(Locale.ROOT);
        return address.endsWith(":25565") ? address.substring(0, address.length() - ":25565".length()) : address;
    }
}

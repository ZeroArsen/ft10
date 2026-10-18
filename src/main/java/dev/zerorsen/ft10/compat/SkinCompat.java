package dev.zerorsen.ft10.compat;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
//? if >=1.21.9 {
import net.minecraft.world.entity.player.PlayerSkin;
//?} else {
/*import net.minecraft.client.resources.PlayerSkin;
*///?}

/**
 * Skins for the HUD's player heads. Only skins the client already has (tab list) are used; nothing is downloaded.
 * {@code PlayerSkin} moved from {@code client.resources} to {@code world.entity.player} in 1.21.9.
 */
final class SkinCompat {
    /** Skins of players who left, so an opponent's head stays after they quit. */
    private static final int REMEMBERED = 64;
    private static final Map<String, PlayerSkin> remembered = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PlayerSkin> eldest) {
            return size() > REMEMBERED;
        }
    };

    private SkinCompat() {}

    /** The player's skin from the tab list, the last one seen, or the default skin for their UUID. */
    static PlayerSkin skin(UUID uuid, String name) {
        String key = uuid != null ? uuid.toString() : "name:" + name.toLowerCase(Locale.ROOT);
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        PlayerInfo info = connection == null ? null
            : uuid != null ? connection.getPlayerInfo(uuid) : connection.getPlayerInfo(name);
        if (info != null) {
            PlayerSkin skin = info.getSkin();
            remembered.put(key, skin);
            return skin;
        }
        PlayerSkin known = remembered.get(key);
        if (known != null) return known;
        return DefaultPlayerSkin.get(uuid != null ? uuid : UUIDUtil.createOfflinePlayerUUID(name));
    }
}

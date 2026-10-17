package dev.zerorsen.ft10.compat;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * Key mapping registration. Categories became {@code KeyMapping.Category} records in 1.21.9 (strings before), and
 * Fabric's helper moved from fabric-key-binding-api to fabric-key-mapping-api in 26.x.
 */
public final class KeyCompat {
    private static KeyMapping.Category category;

    private KeyCompat() {}

    /** An unbound key in the "FT10" category of the controls screen. */
    public static KeyMapping register(String translationKey) {
        if (category == null) category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("ft10", "main"));
        return KeyMappingHelper.registerKeyMapping(
            new KeyMapping(translationKey, InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), category));
    }
}

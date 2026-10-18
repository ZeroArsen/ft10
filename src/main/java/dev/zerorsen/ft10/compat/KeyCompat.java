package dev.zerorsen.ft10.compat;

import com.mojang.blaze3d.platform.InputConstants;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
//?} else {
/*import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
*///?}
import net.minecraft.client.KeyMapping;
//? if >=1.21.9 {
import net.minecraft.resources.Identifier;
//?}

/**
 * Key mapping registration. Categories became {@code KeyMapping.Category} records in 1.21.9 (strings before), and
 * Fabric's helper moved from fabric-key-binding-api to fabric-key-mapping-api in 26.x.
 */
public final class KeyCompat {
    //? if >=1.21.9 {
    private static KeyMapping.Category category;
    //?} else {
    /*private static final String category = "key.category.ft10.main";
    *///?}

    private KeyCompat() {}

    /** An unbound key in the "FT10" category of the controls screen. */
    public static KeyMapping register(String translationKey) {
        //? if >=1.21.9 {
        if (category == null) category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("ft10", "main"));
        //?}
        KeyMapping key = new KeyMapping(translationKey, InputConstants.UNKNOWN.getValue(), category);
        //? if >=26.1 {
        return KeyMappingHelper.registerKeyMapping(key);
        //?} else {
        /*return KeyBindingHelper.registerKeyBinding(key);
        *///?}
    }
}

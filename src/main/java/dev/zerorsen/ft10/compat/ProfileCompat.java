package dev.zerorsen.ft10.compat;

import com.mojang.authlib.GameProfile;
import java.util.UUID;

/** GameProfile became a record (id()/name()) in authlib 7, shipped with 1.21.9. */
public final class ProfileCompat {
    private ProfileCompat() {}

    public static UUID id(GameProfile profile) {
        //? if >=1.21.9 {
        return profile.id();
        //?} else {
        /*return profile.getId();
        *///?}
    }

    public static String name(GameProfile profile) {
        //? if >=1.21.9 {
        return profile.name();
        //?} else {
        /*return profile.getName();
        *///?}
    }
}

package dev.zerorsen.ft10.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Small client calls that moved between versions. */
public final class ClientCompat {
    private ClientCompat() {}

    /** 26.2 moved screen handling from Minecraft to Gui. */
    public static void setScreen(Minecraft minecraft, Screen screen) {
        //? if >=26.2 {
        minecraft.gui.setScreen(screen);
        //?} else {
        /*minecraft.setScreen(screen);
        *///?}
    }

    public static Screen screen(Minecraft minecraft) {
        //? if >=26.2 {
        return minecraft.gui.screen();
        //?} else {
        /*return minecraft.screen;
        *///?}
    }
}

package dev.zerorsen.ft10.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Small client calls that moved between versions. */
public final class ClientCompat {
    private ClientCompat() {}

    /** 26.x moved screen handling from Minecraft to Gui. */
    public static void setScreen(Minecraft minecraft, Screen screen) {
        minecraft.gui.setScreen(screen);
    }

    public static Screen screen(Minecraft minecraft) {
        return minecraft.gui.screen();
    }
}

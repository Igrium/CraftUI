package com.igrium.craftui.impl.input;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;

/**
 * Keeps track of when the cursor should be locked vs unlocked.
 */
public class CursorLockManager {

    @Getter
    @Setter
    private static boolean forceUnlock;

    private static boolean prevForceUnlock;

    public static void onBeginFrame() {
        Minecraft client = Minecraft.getInstance();

        if (forceUnlock != prevForceUnlock) {
            if (forceUnlock) {
                client.mouseHandler.releaseMouse();
            } else {
                setCursorLock(client.mouseHandler, clientWantsLockCursor());
            }
        }
        prevForceUnlock = forceUnlock;
    }

    private static void setCursorLock(MouseHandler mouse, boolean lock) {
        if (lock)
            mouse.grabMouse();
        else
            mouse.releaseMouse();
    }

    public static boolean clientWantsLockCursor() {
        return Minecraft.getInstance().gui.screen() == null;
    }
}

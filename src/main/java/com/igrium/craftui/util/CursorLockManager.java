package com.igrium.craftui.util;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;

/**
 * Keeps track of when the cursor should be locked vs unlocked.
 */
public class CursorLockManager {

    @Getter
    @Setter
    private static boolean forceUnlock;

    private static boolean prevForceUnlock;

    public static void onBeginFrame() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (forceUnlock != prevForceUnlock) {
            if (forceUnlock) {
                client.mouse.unlockCursor();
            } else {
                setCursorLock(client.mouse, clientWantsLockCursor());
            }
        }
        prevForceUnlock = forceUnlock;
    }

    private static void setCursorLock(Mouse mouse, boolean lock) {
        if (lock)
            mouse.lockCursor();
        else
            mouse.unlockCursor();
    }

    private static boolean clientWantsLockCursor() {
        return MinecraftClient.getInstance().currentScreen == null;
    }
}

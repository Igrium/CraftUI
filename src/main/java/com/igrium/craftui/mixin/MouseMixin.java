package com.igrium.craftui.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.igrium.craftui.app.AppManager;
import com.igrium.craftui.app.CraftApp.ViewportBounds;
import com.igrium.craftui.util.MouseUtils;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.InputUtil;

@Mixin(Mouse.class)
public class MouseMixin {

    @Shadow
    @Final
    MinecraftClient client;

    @Shadow
    boolean cursorLocked;

    @Shadow
    double x;

    @Shadow
    double y;

    @Inject(method = "onCursorPos", at = @At("HEAD"))
    void craftui$onCursorPos(long window, double mouseX, double mouseY, CallbackInfo ci,
            @Local(argsOnly = true, ordinal = 0) LocalDoubleRef x, @Local(argsOnly = true, ordinal = 1) LocalDoubleRef y) {
        // Do the if check again because it's easier to mix into the head.
        if (window != MinecraftClient.getInstance().getWindow().getHandle()) {
            return;
        }

        ViewportBounds viewport = AppManager.getCustomViewportBounds();
        if (viewport != null) {
            var newPos = MouseUtils.calculateViewportMouse(client, viewport, x.get(), y.get());
            x.set(newPos.x());
            y.set(newPos.y());
        }
        // if (viewport != null) {
        //     x.set(x.get() + 512);
        //     y.set(y.get() - viewport.y());
        // }
    }

    @Inject(method = "unlockCursor", at = @At("HEAD"), cancellable = true)
    void craftui$unlockCursor(CallbackInfo ci) {
        // Do the if check again because it's easier to mix into the head.
        if (!cursorLocked) {
            return;
        }

        ViewportBounds viewport = AppManager.getCustomViewportBounds();
        if (viewport != null) {

            this.cursorLocked = false;
            this.x = viewport.x() + viewport.width() / 2;
            this.y = viewport.y() + viewport.height() / 2;

            InputUtil.setCursorParameters(client.getWindow().getHandle(), InputUtil.GLFW_CURSOR_NORMAL, x, y);
            
            // For some reason, setCursorParameters attempts to set the cursor pos BEFORE changing its mode, making the x and y args useless.
            GLFW.glfwSetCursorPos(client.getWindow().getHandle(), x, y); 
            ci.cancel();
        }
    }
}

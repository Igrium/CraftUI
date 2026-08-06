package com.igrium.craftui.impl.mixin;

import com.igrium.craftui.impl.input.CursorLockManager;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.igrium.craftui.app.AppManager;
import com.igrium.craftui.app.CraftApp.ViewportBounds;
import com.igrium.craftui.impl.input.MouseUtils;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;

@Mixin(MouseHandler.class)
public class MixinMouseHandler {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private boolean mouseGrabbed;

    @Shadow
    private double xpos;

    @Shadow
    private double ypos;

    @Inject(method = "onMove", at = @At("HEAD"))
    void craftui$onCursorPos(long handle, double mouseX, double mouseY, CallbackInfo ci,
                             @Local(argsOnly = true, name = "xpos") LocalDoubleRef x,
                             @Local(argsOnly = true, name = "ypos") LocalDoubleRef y) {
        // Do the if check again because it's easier to mix into the head.
        if (handle != Minecraft.getInstance().getWindow().handle()) {
            return;
        }

        ViewportBounds viewport = AppManager.getCustomViewportBounds();
        if (viewport != null) {
            var newPos = MouseUtils.calculateViewportMouse(minecraft.getWindow(), viewport, x.get(), y.get());
            x.set(newPos.x());
            y.set(newPos.y());
        }
    }


    @Inject(method = "releaseMouse", at = @At("HEAD"), cancellable = true)
    void craftui$unlockCursor(CallbackInfo ci) {
        // Do the if check again because it's easier to mix into the head.
        if (!mouseGrabbed) {
            return;
        }

        // Fix an issue where keys could get stuck when switching between ui and MC
        // Honestly this should be called in vanilla code. No idea why it's not.
        KeyMapping.releaseAll();

        ViewportBounds viewport = AppManager.getCustomViewportBounds();
        if (viewport != null) {

            this.mouseGrabbed = false;

            this.xpos = (double) viewport.width() / 2 + viewport.x();
            this.ypos = minecraft.getWindow().getScreenHeight() - ((double) viewport.height() / 2 + viewport.y());

            InputConstants.grabOrReleaseMouse(minecraft.getWindow(), InputConstants.CURSOR_NORMAL, xpos, ypos);

            // For some reason, setCursorParameters attempts to set the cursor pos BEFORE changing its mode, making the x and y args useless.
            GLFW.glfwSetCursorPos(minecraft.getWindow().handle(), xpos, ypos);
            ci.cancel();
        }
    }

    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    void craftui$onLockCursor(CallbackInfo ci) {
        if (CursorLockManager.isForceUnlock())
            ci.cancel();
    }

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    void craftui$onMouseButton(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo ci) {
        if (AppManager.wantCaptureMouse()) {
            ci.cancel();
            return;
        }

        MouseUtils.setMousePressed(action == GLFW.GLFW_PRESS);
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    void craftui$onMouseScroll(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        if (AppManager.wantCaptureMouse())
            ci.cancel();
    }
}

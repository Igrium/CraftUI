package com.igrium.craftui.impl.mixin;

import com.igrium.craftui.impl.input.CursorLockManager;
import net.minecraft.client.option.KeyBinding;
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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.util.InputUtil;

@Mixin(Mouse.class)
public class MouseMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private boolean cursorLocked;

    @Shadow
    private double x;

    @Shadow
    private double y;

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

    @Inject(method = "updateMouse", at = @At("HEAD"), cancellable = true)
    void craftui$onUpdateMouse(CallbackInfo ci) {
        if (AppManager.wantCaptureMouse())
            ci.cancel();
    }

    @Inject(method = "unlockCursor", at = @At("HEAD"), cancellable = true)
    void craftui$unlockCursor(CallbackInfo ci) {
        // Do the if check again because it's easier to mix into the head.
        if (!cursorLocked) {
            return;
        }

        // Fix an issue where keys could get stuck when switching between ui and MC
        // Honestly this should be called in vanilla code. No idea why it's not.
        KeyBinding.unpressAll();

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

    @Inject(method = "lockCursor", at = @At("HEAD"), cancellable = true)
    void craftui$onLockCursor(CallbackInfo ci) {
        if (CursorLockManager.isForceUnlock())
            ci.cancel();
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    void craftui$onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        if (AppManager.wantCaptureMouse()) {
            ci.cancel();
            return;
        }

        MouseUtils.setMousePressed(action == GLFW.GLFW_PRESS);
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    void craftui$onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (AppManager.wantCaptureMouse())
            ci.cancel();
    }
}

package com.igrium.craftui.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.igrium.craftui.AppManager;
import com.igrium.craftui.CraftApp.ViewportBounds;
import com.igrium.craftui.util.MouseUtils;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;

@Mixin(Mouse.class)
public class MouseMixin {

    @Shadow
    @Final
    MinecraftClient client;

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
}

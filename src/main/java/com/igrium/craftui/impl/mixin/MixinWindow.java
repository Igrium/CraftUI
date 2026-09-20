package com.igrium.craftui.impl.mixin;

import com.igrium.craftui.api.app.CraftApp;
import com.igrium.craftui.api.app.CraftApp.ViewportBounds;
import com.igrium.craftui.impl.mixin_helper.ExtWindow;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(Window.class)
public class MixinWindow implements ExtWindow {

    @Unique
    private @Nullable ViewportBounds viewportBounds;

    @Shadow @Final
    private WindowEventHandler eventHandler;
    
    @Inject(method = "getWidth", at = @At("HEAD"), cancellable = true)
    public void getWidth(CallbackInfoReturnable<Integer> cir) {
        if (viewportBounds != null) {
            cir.setReturnValue(viewportBounds.scaled().width());
        }
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    public void getHeight(CallbackInfoReturnable<Integer> cir) {
        if (viewportBounds != null) {
            cir.setReturnValue(viewportBounds.scaled().height());
        }
    }

    @Override
    public void craftui$getViewportBounds(@Nullable ViewportBounds viewportBounds, boolean updateListeners) {
        var prevBounds = this.viewportBounds;
        this.viewportBounds = viewportBounds;

        if (updateListeners && !Objects.equals(prevBounds, viewportBounds)) {
            eventHandler.framebufferSizeChanged();
        }
    }

    @Override
    public @Nullable ViewportBounds craftui$getViewportBounds() {
        return viewportBounds;
    }
}

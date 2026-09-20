package com.igrium.craftui.impl.mixin;

import com.igrium.craftui.api.app.CraftApp.ViewportBounds;
import com.igrium.craftui.impl.mixin_helper.ExtWindow;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(Window.class)
public class MixinWindow implements ExtWindow {

    @Unique
    private @Nullable ViewportBounds viewportBounds;

    @Shadow @Final
    private WindowEventHandler eventHandler;

    @Shadow
    private int guiScale;
    @Shadow
    private int guiScaledWidth;
    @Shadow
    private int guiScaledHeight;

    @Inject(method = "getWidth", at = @At("HEAD"), cancellable = true)
    public void getWidth(CallbackInfoReturnable<Integer> cir) {
        if (viewportBounds != null) {
            cir.setReturnValue(viewportBounds.width());
        }
    }

    @Inject(method = "getHeight", at = @At("HEAD"), cancellable = true)
    public void getHeight(CallbackInfoReturnable<Integer> cir) {
        if (viewportBounds != null) {
            cir.setReturnValue(viewportBounds.height());
        }
    }

    // Needs re-implementing because the default implementation references the fields directly
    @Inject(method = "calculateScale", at = @At("HEAD"), cancellable = true)
    public void calculateScale(int maxScale, boolean enforceUnicode, CallbackInfoReturnable<Integer> cir) {
        if (viewportBounds == null) {
            return;
        }

        int fbWidth = viewportBounds.width();
        int fbHeight = viewportBounds.height();

        int scale = 1;

        while (
                scale != maxScale
                        && scale < fbWidth
                        && scale < fbHeight
                        && fbWidth / (scale + 1) >= 320
                        && fbHeight / (scale + 1) >= 240
        ) {
            scale++;
        }

        if (enforceUnicode && scale % 2 != 0) {
            scale++;
        }


        cir.setReturnValue(scale);
    }

    @Inject(method = "setGuiScale", at = @At("HEAD"), cancellable = true)
    public void setGuiScale(int guiScale, CallbackInfo ci) {
        if (viewportBounds == null) {
            return;
        }

        this.guiScale = guiScale;
        this.guiScaledWidth = (int) Math.ceil((double) viewportBounds.width() / guiScale);
        this.guiScaledHeight = (int) Math.ceil((double) viewportBounds.height() / guiScale);

        ci.cancel();
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

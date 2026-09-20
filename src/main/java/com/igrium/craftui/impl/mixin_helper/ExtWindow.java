package com.igrium.craftui.impl.mixin_helper;

import com.igrium.craftui.api.app.CraftApp.ViewportBounds;
import com.mojang.blaze3d.platform.Window;
import org.jetbrains.annotations.Nullable;

public interface ExtWindow {
    void craftui$getViewportBounds(@Nullable ViewportBounds viewportBounds, boolean updateListeners);
    @Nullable ViewportBounds craftui$getViewportBounds();

    static void setViewportBounds(Window window, @Nullable ViewportBounds viewportBounds, boolean updateListeners) {
        ((ExtWindow) (Object) window).craftui$getViewportBounds(viewportBounds, updateListeners);
    }

    static @Nullable ViewportBounds getViewportBounds(Window window) {
        return ((ExtWindow) (Object) window).craftui$getViewportBounds();
    }
}

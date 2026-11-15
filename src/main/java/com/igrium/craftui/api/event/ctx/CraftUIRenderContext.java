package com.igrium.craftui.api.event.ctx;

import com.igrium.craftui.api.app.CraftApp;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * The current CraftUI render state for use in event listeners
 */
@ApiStatus.NonExtendable
public interface CraftUIRenderContext extends BaseCraftUIRenderContext {
    /**
     * Custom viewport bounds for this frame
     */
    @Nullable CraftApp.ViewportBounds getViewportBounds();

    boolean isCleanupFrame();
}



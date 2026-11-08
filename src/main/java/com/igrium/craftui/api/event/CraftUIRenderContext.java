package com.igrium.craftui.api.event;

import com.igrium.craftui.api.app.CraftApp;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;

/**
 * The current CraftUI render state for use in event listeners
 */
@ApiStatus.NonExtendable
public interface CraftUIRenderContext {
    /**
     * All apps which are open
     */
    Collection<? extends CraftApp> getApps();
}



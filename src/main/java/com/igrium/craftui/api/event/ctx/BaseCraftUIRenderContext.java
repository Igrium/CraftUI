package com.igrium.craftui.api.event.ctx;

import com.igrium.craftui.api.app.CraftApp;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;

@ApiStatus.NonExtendable
public interface BaseCraftUIRenderContext {
    /**
     * Minecraft client instance
     */
    Minecraft getClient();

    /**
     * All apps which are open
     */
    Collection<? extends CraftApp> getApps();
}

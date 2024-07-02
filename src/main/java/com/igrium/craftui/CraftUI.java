package com.igrium.craftui;

import com.igrium.craftui.event.FontReloadCallback;
import com.igrium.craftui.font.Fonts;
import com.igrium.craftui.font.ImFontManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

public class CraftUI implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(ImFontManager.getInstance());
        FontReloadCallback.EVENT.register(Fonts::reload);
    }
    
}

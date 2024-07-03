package com.igrium.craftui;

import com.igrium.craftui.config.CraftUIConfig;
import com.igrium.craftui.file.FileDialogs;
import com.igrium.craftui.font.ImFontManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

public class CraftUI implements ClientModInitializer {

    private static CraftUIConfig config = new CraftUIConfig();

    public static CraftUIConfig getConfig() {
        return config;
    }

    public static void setConfig(CraftUIConfig config) {
        CraftUI.config = config;
        applyConfig();
    }

    /**
     * Apply the current config to all relevent parts of the code. Call after any
     * config update.
     */
    public static void applyConfig() {
        FileDialogs.setPreferNative(config.preferNativeFileDialog());;
    }

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(ImFontManager.getInstance());
    }
    
}

package com.igrium.craftui;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.craftui.config.CraftUIConfig;
import com.igrium.craftui.file.FileDialogs;
import com.igrium.craftui.font.ImFontManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Util;

public class CraftUI implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("CraftUI");

    public static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("craftui.json");
    private final static CraftUIConfig config = new CraftUIConfig();
    

    public static CraftUIConfig getConfig() {
        return config;
    }

    public static void setConfig(CraftUIConfig config) {
        CraftUI.config.copyFrom(config);
        applyConfig(true);
    }

    public static CompletableFuture<Void> reloadConfigAsync() {
        return CompletableFuture.runAsync(CraftUI::reloadConfigSync, Util.getIoWorkerExecutor());
    }

    public static synchronized void reloadConfigSync() {
        try {
            config.importConfig(CONFIG_FILE);
            RenderSystem.recordRenderCall(() -> applyConfig(false));
            LOGGER.info("Loaded CraftUI config from {}", CONFIG_FILE);
        } catch (Exception e) {
            LOGGER.error("Error loading CraftUI config.", e);
        }
    }

    /**
     * Apply the config to all relevant parts of the code.
     * 
     * @param save Whether to save the config to disk. Saving happens async.
     */
    public static void applyConfig(boolean save) {
        RenderSystem.assertOnRenderThreadOrInit();
        // Apply config shit here
        FileDialogs.clearImpl();
        LOGGER.info("Applied CraftUI config update");
        if (save) {
            saveConfigAsync();
        }
    }

    public static CompletableFuture<Void> saveConfigAsync() {
        return CompletableFuture.runAsync(CraftUI::saveConfigSync, Util.getIoWorkerExecutor());
    }

    public static synchronized void saveConfigSync() {
        try(Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
            writer.write(config.toJsonString());
            LOGGER.info("Saved CraftUI config to {}", CONFIG_FILE);
        } catch (Exception e) {
            LOGGER.error("Error saving CraftUI config.", e);
        }
    }

    @Override
    public void onInitializeClient() {
        reloadConfigAsync();
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(ImFontManager.getInstance());
    }
    
}

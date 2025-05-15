package com.igrium.craftui;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import com.igrium.craftui.config.CraftUIConfigHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.craftui.config.CraftUIConfig;
import com.igrium.craftui.dev.commands.CraftUICommand;
import com.igrium.craftui.file.FileDialogs;
import com.igrium.craftui.font.ImFontManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Util;

public class CraftUI implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("CraftUI");

    public static CraftUIConfig getConfig() {
        return CraftUIConfigHandler.getConfig();
    }

    @Override
    public void onInitializeClient() {
        CraftUIConfigHandler.initConfig();
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(ImFontManager.getInstance());

        if (getConfig().isEnableDebugCommands()) {
            ClientCommandRegistrationCallback.EVENT.register(CraftUICommand::register);
        }
    }
    
}

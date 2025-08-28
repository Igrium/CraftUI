package com.igrium.craftui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.igrium.craftui.impl.CraftUIConfigCallback;
import com.igrium.craftui.impl.config.IniSettingsManager;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.igrium.craftui.config.CraftUIConfig;
import com.igrium.craftui.impl.commands.CraftUICommand;
import com.igrium.craftui.impl.font.ImFontManager;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;

import java.nio.file.Files;
import java.nio.file.Path;

public class CraftUI implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("CraftUI");

    @Override
    public void onInitializeClient() {
        initConfig();

        if (getConfig().isLayoutPersistent()) {
            IniSettingsManager.readFromDisk();
        }

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(ImFontManager.getInstance());

        if (getConfig().isEnableDebugCommands()) {
            ClientCommandRegistrationCallback.EVENT.register(CraftUICommand::register);
        }
    }

    // CONFIG
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("craftui.json");

    /**
     * The primary configuration file for CraftUI
     */
    @Getter @NotNull
    private static final CraftUIConfig config = new CraftUIConfig();

    /**
     * Load the CraftUI config from file. Log an error if unable to load.
     * @return If the config loaded successfully.
     */
    public static boolean reloadConfig() {
        LOGGER.info("Loading CraftUI config from {}", CONFIG_FILE);
        try (var reader = Files.newBufferedReader(CONFIG_FILE)) {
            config.loadConfig(reader);
            CraftUIConfigCallback.EVENT.invoker().onUpdateConfig(config);
            return true;
        } catch (Exception e) {
            LOGGER.error("Error loading CraftUI config", e);
            return false;
        }
    }

    /**
     * Save the CraftUI config to file. Log an error if unable to save.
     * @return If the config saved successfully.
     */
    public static boolean saveConfig() {
        CraftUIConfigCallback.EVENT.invoker().onUpdateConfig(config);
        LOGGER.info("Saving CraftUI config to {}", CONFIG_FILE);
        try (var writer = Files.newBufferedWriter(CONFIG_FILE)) {
            config.saveConfig(writer);
            return true;
        } catch (Exception e) {
            LOGGER.error("Error saving CraftUI config", e);
            return false;
        }
    }

    private static void initConfig() {
        if (Files.isRegularFile(CONFIG_FILE)) {
            reloadConfig();
        } else {
            LOGGER.info("No CraftUI config found. Initializing...");
            saveConfig();
        }
    }
}

package com.igrium.craftui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.igrium.craftui.CraftUI;
import com.igrium.craftui.file.FileDialogs;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Handles reloading config and shit.
 */
public class CraftUIConfigHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("craftui.json");

    @NotNull
    @Getter
    private static final CraftUIConfig config = new CraftUIConfig();

    /**
     * Update the CraftUI config with new values.
     * @param otherConfig Config to copy from.
     */
    public static void updateConfig(CraftUIConfig otherConfig) {
        config.copyFrom(otherConfig);
    }

    /**
     * Save the CraftUI config to file. Log an error if unable to save.
     */
    public static void saveConfig() {
        try(var writer = Files.newBufferedWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
            FileDialogs.clearImpl();
        } catch (IOException e) {
            CraftUI.LOGGER.error("Error saving CraftUI config:", e);
        }
    }

    /**
     * Load the CraftUI config from file. Log an error if unable to load.
     */
    public static void loadConfig() {
        CraftUIConfig otherConfig;
        try(var reader = Files.newBufferedReader(CONFIG_FILE)) {
            otherConfig = GSON.fromJson(reader, CraftUIConfig.class);
        } catch (IOException e) {
            CraftUI.LOGGER.error("Error saving CraftUI config:", e);
            return;
        };
        config.copyFrom(otherConfig);
    }

    /**
     * Called upon the mod startup. If the CraftUI config file exists, load it.
     * Otherwise, create one with the default values.
     */
    public static void initConfig() {
        if (Files.isRegularFile(CONFIG_FILE)) {
            CraftUI.LOGGER.info("Loading CraftUI config from {}", CONFIG_FILE);
            loadConfig();
        } else {
            CraftUI.LOGGER.info("No CraftUI config found. Initializing...");
            saveConfig();
        }
    }
}

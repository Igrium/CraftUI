package com.igrium.craftui.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.igrium.craftui.CraftUI;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;

/**
 * Configuration settings for CraftUI
 * @see CraftUI#getConfig()
 */
@Setter @Getter
public final class CraftUIConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Prefer the native system file dialog over the ImGui one.
     */
    private boolean preferNativeFileDialog = true;

    /**
     * If set, widget windows may be moved outside the primary game window. Can cause unexpected behavior.
     */
    private boolean enableViewports = false;

    /**
     * Save the ImGui layout to disk.
     */
    private boolean layoutPersistent = true;

    /**
     * Enable the craftui client command, exposing a variety of debug features.
     */
    private boolean enableDebugCommands = FabricLoader.getInstance().isDevelopmentEnvironment();

    public void copyFrom(CraftUIConfig other) {
        this.preferNativeFileDialog = other.preferNativeFileDialog;
        this.enableViewports = other.enableViewports;
        this.enableDebugCommands = other.enableDebugCommands;
    }

    public void loadConfig(Reader reader) {
        CraftUIConfig otherConfig = GSON.fromJson(reader, CraftUIConfig.class);
        this.copyFrom(otherConfig);
    }

    public void saveConfig(Writer writer) {
        GSON.toJson(this, writer);
    }
}

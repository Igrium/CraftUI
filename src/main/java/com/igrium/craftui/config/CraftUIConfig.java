package com.igrium.craftui.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.loader.api.FabricLoader;

@Setter
@Getter
public final class CraftUIConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Prefer the native system file dialog over the ImGUI one.
     */
    private boolean preferNativeFileDialog = true;

    /**
     * If set, widget windows may be moved outside the primary game window. Can cause unexpected behavior.
     */
    private boolean enableViewports = false;

    /**
     * Enable the craftui client command, exposing a variety of debug features.
     */
    private boolean enableDebugCommands = FabricLoader.getInstance().isDevelopmentEnvironment();

    public void copyFrom(CraftUIConfig other) {
        this.preferNativeFileDialog = other.preferNativeFileDialog;
        this.enableViewports = other.enableViewports;
        this.enableDebugCommands = other.enableDebugCommands;
    }
}

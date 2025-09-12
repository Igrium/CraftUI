package com.igrium.craftui.impl.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.JsonAdapter;
import com.igrium.craftui.CraftUI;
import com.igrium.craftui.file.FileDialogs;
import com.igrium.craftui.impl.util.IdentifierJsonAdapter;
import com.igrium.craftui.style.CraftUIStyles;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.io.Writer;

/**
 * Configuration settings for CraftUI
 * @see CraftUI#getConfig()
 */
@Setter @Getter
public final class CraftUIConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @JsonAdapter(IdentifierJsonAdapter.class)
    private Identifier style = CraftUIStyles.DARK;

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

    public void applyConfig() {
        FileDialogs.clearImpl();
        CraftUIStyles.setActiveStyle(style);
    }
}

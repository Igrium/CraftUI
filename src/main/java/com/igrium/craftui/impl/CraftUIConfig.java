package com.igrium.craftui.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.JsonAdapter;
import com.igrium.craftui.api.file.FileDialogs;
import com.igrium.craftui.impl.util.JsonAdapters;
import com.igrium.craftui.api.style.CraftUIStyles;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import java.io.Reader;
import java.io.Writer;

/**
 * Configuration settings for CraftUIEntrypoint
 * @see CraftUIEntrypoint#getConfig()
 */
@Setter @Getter
public final class CraftUIConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @JsonAdapter(JsonAdapters.IdentifierJsonAdapter.class)
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
        this.style = other.style;
        this.layoutPersistent = other.layoutPersistent;
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

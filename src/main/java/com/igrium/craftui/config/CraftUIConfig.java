package com.igrium.craftui.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

public final class CraftUIConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean preferNativeFileDialog = true;

    public boolean preferNativeFileDialog() {
        return preferNativeFileDialog;
    }

    public void setPreferNativeFileDialog(boolean preferNativeFiloeDialog) {
        this.preferNativeFileDialog = preferNativeFiloeDialog;
    }

    private boolean enableViewports = false;

    public boolean enableViewports() {
        return enableViewports;
    }

    public void setEnableViewports(boolean enableViewports) {
        this.enableViewports = enableViewports;
    }

    public void copyFrom(CraftUIConfig other) {
        this.preferNativeFileDialog = other.preferNativeFileDialog;
        this.enableViewports = other.enableViewports;
    }

    public String toJsonString() {
        return GSON.toJson(this);
    }


    /**
     * Read the config from a file and apply it to this config instance.
     * 
     * @param path File to read.
     * @throws IOException        If an IO exception occurs reading the file.
     * @throws JsonParseException If the json file cannot be parsed.
     */
    public void importConfig(Path path) throws IOException, JsonParseException {
        this.copyFrom(loadConfig(path));
    }

    /**
     * Read the config from file and return it as a new config instance.
     * 
     * @param path File to read.
     * @return Config instance.
     * @throws IOException        If an IO exception occurs reading the file
     * @throws JsonParseException If the json file cannot be parsed.
     */
    public static CraftUIConfig loadConfig(Path path) throws IOException, JsonParseException {
        try (Reader reader = Files.newBufferedReader(path)) {
            return GSON.fromJson(reader, CraftUIConfig.class);
        }
    }
}

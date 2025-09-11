package com.igrium.craftui.impl.layout;

import com.igrium.craftui.CraftUI;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

@Deprecated
public class IniSettingsManager {
    @Getter @Setter @NonNull
    private static String imGuiSettings = "";

    @Nullable
    private static CompletableFuture<?> diskSaveFuture;

    @Nullable
    private static CompletableFuture<?> diskReadFuture;

    // Essentially a mutex on the file itself
    private static final Object mutex = new Object();

    public static Path getConfigFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("imgui.ini");
    }

    public static synchronized CompletableFuture<?> saveToDisk() {
        if (diskSaveFuture != null && !diskSaveFuture.isDone()) {
            CraftUI.LOGGER.warn("IMGUI settings are already being saved.");
            return diskSaveFuture;
        }
        diskSaveFuture = CompletableFuture.runAsync(IniSettingsManager::saveToDiskSync, Util.getIoWorkerExecutor());
        return diskSaveFuture;
    }

    public static void saveToDiskSync() {
        synchronized (mutex) {
            try {
                Files.writeString(getConfigFile(), imGuiSettings);
            } catch (IOException e) {
                CraftUI.LOGGER.error("Error saving imgui config: ", e);
            }
        }
    }

    public static synchronized CompletableFuture<?> readFromDisk() {
        if (diskReadFuture != null && !diskReadFuture.isDone()) {
            CraftUI.LOGGER.warn("Imgui settings are already being loaded.");
            return diskReadFuture;
        }
        diskReadFuture = CompletableFuture.runAsync(IniSettingsManager::readFromDiskSync, Util.getIoWorkerExecutor());
        return diskReadFuture;
    }

    public static void readFromDiskSync() {
        CraftUI.LOGGER.info("Loading ImGui settings.");
        Path readPath = getConfigFile();
        synchronized (mutex) {
            if (!Files.isRegularFile(readPath))
                return;

            try {
                imGuiSettings = Files.readString(readPath);
            } catch (IOException e) {
                CraftUI.LOGGER.error("Error reading imgui config: ", e);
            }
        }
    }

    public static void clearIniFile() {
        synchronized (mutex) {
            try {
                Files.deleteIfExists(getConfigFile());
            } catch (IOException e) {
                CraftUI.LOGGER.error("Error removing imgui.ini: ", e);
            }
        }
    }
}

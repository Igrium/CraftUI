package com.igrium.craftui.impl.style;

import com.igrium.craftui.CraftUI;
import com.igrium.craftui.style.CraftUILayouts;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class LayoutManager implements IdentifiableResourceReloadListener {
    private static LayoutManager instance;

    public static LayoutManager getInstance() {
        if (instance == null) {
            instance = new LayoutManager();
        }
        return instance;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("CraftUI LayoutManager");

    private LayoutManager() {};

    private final Map<Identifier, String> nativeLayouts = new ConcurrentHashMap<>();
    private final Map<Identifier, String> userLayouts = new ConcurrentHashMap<>();

    @Getter @NotNull
    private Identifier activeLayout = CraftUILayouts.DEFAULT;

    @Getter @Setter
    private boolean layoutUpdate = true;

    public void setActiveLayout(@Nullable Identifier layout) {
        if (layout == null) layout = CraftUILayouts.DEFAULT;
        if (this.activeLayout.equals(layout)) return;

        this.activeLayout = layout;
        setLayoutUpdate(true);
    }

    public @NotNull String getActiveLayoutData() {
        var data = getLayoutData(getActiveLayout());
        return data != null ? data : "";
    }

    public @Nullable String getNativeLayoutData(Identifier id) {
        return nativeLayouts.get(id);
    }

    public @Nullable String getUserLayoutData(Identifier id) {
        return userLayouts.get(id);
    }

    public @Nullable String getLayoutData(Identifier id) {
        var d = getUserLayoutData(id);
        return d != null ? d : getNativeLayoutData(id);
    }

    public void setUserLayoutData(Identifier layoutId, String data, boolean save) {
        userLayouts.put(layoutId, data);
//        if (activeLayout.equals(layoutId)) {
//            setLayoutUpdate(true);
//        }

        if (save && CraftUI.getConfig().isLayoutPersistent()) {
            saveUserLayout(layoutId);
        }
    }

    /**
     * Save updated user layout data to disk.
     */
    public void saveUserLayoutData(String data) {
        setUserLayoutData(getActiveLayout(), data, true);
    }

    public void resetLayout(Identifier id) {
        userLayouts.remove(id);
        try {
            Files.deleteIfExists(getUserLayoutPath(id));
        } catch (IOException e) {
            LOGGER.error("Error removing user layout: ", e);
        }
        if (getActiveLayout().equals(id)) {
            setLayoutUpdate(true);
        }
    }

    public void resetLayouts() {
        userLayouts.clear();
        try(var paths = Files.walk(FabricLoader.getInstance().getConfigDir().resolve("craftui/layouts"))) {
            paths.sorted(Comparator.reverseOrder()).forEach(LayoutManager::deleteSneaky);
        } catch (IOException e) {
            LOGGER.error("Error deleting user layouts: ", e);
        }
        setLayoutUpdate(true);
    }

    private static void deleteSneaky(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
    }

    private void loadUserLayouts() {
        userLayouts.clear();
        Path rootDir = FabricLoader.getInstance().getConfigDir().resolve("craftui/layouts");
        if (!Files.isDirectory(rootDir)) return;

        try (var stream = Files.walk(rootDir).filter(p -> p.toString().endsWith(".ini") && Files.isRegularFile(p))) {
            stream.forEach(file -> {
                Path relative = rootDir.relativize(file);
                if (relative.getNameCount() < 2) {
                    LOGGER.warn("Skipping layout file not inside a namespace folder: {}", file);
                    return;
                }

                String namespace = relative.getName(0).toString();
                Path localPath = relative.subpath(1, relative.getNameCount());

                String idPath = FilenameUtils.removeExtension(localPath.toString()).replace('\\', '/');

                if (idPath.isEmpty()) {
                    return;
                }

                Identifier id;
                try {
                    id = Identifier.of(namespace, idPath);
                } catch (InvalidIdentifierException e) {
                    LOGGER.error(e.getMessage());
                    return;
                }

                try {
                    String contents = Files.readString(file);
                    userLayouts.put(id, contents);
                } catch (IOException e) {
                    LOGGER.error("Error reading user layout {}: ", file, e);
                }
            });
            setLayoutUpdate(true);
        } catch (Exception e) {
            LOGGER.error("Error loading user layouts: ", e);
        }
    }

    private static Path getUserLayoutPath(Identifier layoutId) {
        return FabricLoader.getInstance().getConfigDir().resolve("craftui/layouts")
                .resolve(layoutId.getNamespace()).resolve(layoutId.getPath() + ".ini");
    }

    private void saveUserLayout(Identifier layoutId) {
        String contents = getUserLayoutData(layoutId);
        if (contents == null || contents.isEmpty()) {
            LOGGER.warn("Unable to save user layout {} because it does not exist.", layoutId);
            return;
        }

        Path path = getUserLayoutPath(layoutId);

        try {
            Files.createDirectories(path.getParent());

            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                writer.write(contents);
            }

            LOGGER.debug("Saved user layout to {}", path);
        } catch (IOException e) {
            LOGGER.error("Error saving user layout {}: ", layoutId, e);
        }
    }

    private void loadNativeLayouts(ResourceManager manager) {
        nativeLayouts.clear();
        for (var entry : manager.findResources("layouts", id -> id.getPath().endsWith(".ini")).entrySet()) {
            Identifier filename = entry.getKey();
            String filepath = FilenameUtils.removeExtension(filename.getPath().substring("layouts/".length()));

            Identifier id = Identifier.of(filename.getNamespace(), filepath);
            LOGGER.debug("Loading IMGUI layout from {} as {}", filename, id);

            try (var reader = entry.getValue().getReader()) {
                String text = reader.lines().collect(Collectors.joining(System.lineSeparator()));
                nativeLayouts.put(id, text);
            } catch (Exception e) {
                LOGGER.error("Error reading layout {}: ", id, e);
            }
        }
        setLayoutUpdate(true);
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("craftui:layouts");
    }

    @Override
    public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager, Executor prepareExecutor, Executor applyExecutor) {
        nativeLayouts.clear();
        return CompletableFuture.runAsync(() -> this.loadNativeLayouts(manager), prepareExecutor)
                .thenRun(this::loadUserLayouts)
                .thenCompose(synchronizer::whenPrepared);
    }
}

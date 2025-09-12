package com.igrium.craftui.impl.style;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.igrium.craftui.style.CraftUIStyle;
import com.igrium.craftui.style.CraftUIStyles;
import com.igrium.craftui.impl.util.JsonUtils;
import imgui.ImGuiStyle;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class StyleManager implements IdentifiableResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("CraftUI StyleManager");

    private static final Gson GSON = new GsonBuilder().create();


    /**
     * Empty IMGUI styles
     */
    private static final CraftUIStyle NATIVE = new CraftUIStyle();
    // Used as base for all styles
    private static final JsonObject NATIVE_JSON;
    static {
        NATIVE.fromNativeStyle(new ImGuiStyle());
        NATIVE_JSON = GSON.toJsonTree(NATIVE).getAsJsonObject();

    }

    private static StyleManager instance;

    public static StyleManager getInstance() {
        if (instance == null)
            instance = new StyleManager();
        return instance;
    }

    private StyleManager() {}

    @Getter @Setter
    private boolean wantStyleUpdate = true;

    private final Map<Identifier, CraftUIStyle> styles = new HashMap<>();

    public @Nullable CraftUIStyle getStyle(Identifier id) {
        return styles.get(id);
    }

    @Getter @NonNull
    private Identifier activeStyle = CraftUIStyles.DARK;

    public void setActiveStyle(@NonNull Identifier activeStyle) {
        if (activeStyle != this.activeStyle) {
            this.activeStyle = activeStyle;
            wantStyleUpdate = true;
        }
    }


    public @NonNull CraftUIStyle getActiveStyleData() {
        CraftUIStyle active = getStyle(getActiveStyle());
        if (active == null) {
            LOGGER.warn("Unknown style: {}", getActiveStyle());
            return NATIVE;
        } else {
            return active;
        }
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("craftui:stylemanager");
    }


    @Override
    public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager, Executor prepareExecutor, Executor applyExecutor) {
        List<CompletableFuture<?>> futures = new ArrayList<>();

        final Map<Identifier, JsonObject> parsedJson = new ConcurrentHashMap<>();

        // Used as base for all styles

        for (var entry : manager.findAllResources("ui/styles", p -> p.toString().endsWith(".json")).entrySet()) {
            futures.add(CompletableFuture.runAsync(() -> {
                JsonObject obj = null;
                for (Resource r : entry.getValue()) {
                    try(var reader = r.getReader()) {
                        var obj2 = GSON.fromJson(reader, JsonObject.class);

                        if (obj == null) {
                            obj = obj2;
                        } else {
                            JsonUtils.extendJsonObject(obj, obj2, JsonUtils.ConflictStrategy.PREFER_SECOND);
                        }

                    } catch (Exception e) {
                        LOGGER.error("Error loading style {} from pack {}: ", entry.getKey(), r.getPackId(), e);
                    }
                }

                if (obj != null) {
                    parsedJson.put(getStyleId(entry.getKey()), obj);
                }
            }, prepareExecutor));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenCompose(synchronizer::whenPrepared).thenRunAsync(() -> {
            styles.clear();
            for (var entry : parsedJson.entrySet()) {
                try {
                    JsonObject json = entry.getValue();
                    applyParents(json, parsedJson, new HashSet<>());
                    styles.put(entry.getKey(), GSON.fromJson(json, CraftUIStyle.class));
                } catch (Exception e) {
                    LOGGER.error("Error parsing json for style {}: ", entry.getKey(), e);
                }
            }
            setWantStyleUpdate(true);
        }, applyExecutor);
    }

    private Identifier getStyleId(Identifier fileId) {
        String path = FilenameUtils.removeExtension(fileId.getPath().substring("ui/styles/".length()));
        return Identifier.of(fileId.getNamespace(), path);
    }

    private static void applyParents(JsonObject obj, Map<Identifier, JsonObject> allValues, Set<Identifier> usedIdentifiers) {
        if (obj.has("parent")) {
            Identifier parentName = Identifier.of(obj.get("parent").getAsString());
            if (usedIdentifiers.contains(parentName)) {
                throw new IllegalStateException("Circular style dependency: " + parentName);
            }
            obj.remove("parent");

            JsonObject parent = allValues.get(parentName);
            if (parent == null) {
                LOGGER.warn("Tried to load non-existent parent style: {}", parentName);
                return;
            }

            usedIdentifiers.add(parentName);
            applyParents(parent, allValues, usedIdentifiers);

            JsonUtils.extendJsonObject(obj, parent, JsonUtils.ConflictStrategy.PREFER_FIRST);
        } else {
            JsonUtils.extendJsonObject(obj, NATIVE_JSON, JsonUtils.ConflictStrategy.PREFER_FIRST);
        }
    }
}

package com.igrium.craftui.impl.style;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

public class StyleManager implements IdentifiableResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("CraftUI StyleManager");

    private StyleManager instance;

    public StyleManager getInstance() {
        if (instance == null)
            instance = new StyleManager();
        return instance;
    }

    private StyleManager() {}

    @Getter @Setter
    private boolean wantsUpdateStyle;

    private final Map<Identifier, CraftUIStyle> styles = new ConcurrentHashMap<>();

    @Override
    public Identifier getFabricId() {
        return Identifier.of("craftui:styleManager");
    }

    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager, Executor prepareExecutor, Executor applyExecutor) {
        styles.clear();
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (var entry : manager.findAllResources("ui/styles/", p -> p.toString().endsWith(".json")).entrySet()) {
            futures.add(CompletableFuture.runAsync(() -> {
                CraftUIStyle style = null;
                for (Resource r : entry.getValue()) {
                    try (var reader = r.getReader()) {

                        CraftUIStyle s = GSON.fromJson(reader, CraftUIStyle.class);
                        if (style != null) {
                            style.appendAll(s);
                        } else {
                            style = s;
                        }

                    } catch (IOException e) {
                        LOGGER.error("Unable to read style file {} from pack {}: ", entry.getKey(), r.getPackId(), e);
                    }
                }

                if (style != null) {
                    styles.put(entry.getKey(), style);
                }
            }, prepareExecutor));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenCompose(synchronizer::whenPrepared);
    }
}

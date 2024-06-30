package com.igrium.craftui.util;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import imgui.ImFont;
import imgui.ImFontAtlas;
import imgui.ImGui;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

/**
 * Deals with loading & caching ImGui fonts from resourcepacks.
 */
public class ImFontManager implements IdentifiableResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImFontManager.class);

    private static BiMap<Identifier, ImFont> fonts = HashBiMap.create();

    @Override
    public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager, Profiler prepareProfiler,
            Profiler applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
        
        var resources = manager.findResources("fonts", id -> id.getPath().endsWith(".ttf"));
        Map<Identifier, byte[]> buffers = new ConcurrentHashMap<>();
        List<CompletableFuture<?>> futures = new ArrayList<>(resources.size());
        
        // Load fonts from file
        for (var entry : resources.entrySet()) {
            futures.add(CompletableFuture.runAsync(() -> {
                
                try(InputStream in = new BufferedInputStream(entry.getValue().getInputStream())) {
                    byte[] buffer = in.readAllBytes();
                    buffers.put(entry.getKey(), buffer);
                } catch (Exception e) {
                    LOGGER.error("Error loading font " + entry.getKey(), e);
                }

            }, prepareExecutor));
        }

        // Render fonts to texture
        var combined = CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
        return combined.thenCompose(synchronizer::whenPrepared).thenAcceptAsync(v -> {
            ImFontAtlas atlas = ImGui.getIO().getFonts();
            atlas.clearFonts();

            for (ImFont font : fonts.values()) {
                font.destroy();
            }
            fonts.clear();

            for (var entry : buffers.entrySet()) {
                fonts.put(entry.getKey(), atlas.addFontFromMemoryTTF(entry.getValue(), 16));
            }
        });
    }

    @Override
    public Identifier getFabricId() {
        return new Identifier("craftui:fonts");
    }
    
    /**
     * Get a font from the resourcepack.
     * 
     * @param font Font ID.
     * @return The font, or the default font if is not loaded. <strong>Do not store
     *         between frames! The instance will change on resourcepack
     *         reload!</strong>
     */
    public static ImFont getFont(Identifier font) {
        return fonts.get(font);
    }
}

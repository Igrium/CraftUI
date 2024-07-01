package com.igrium.craftui.font;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;
import com.igrium.craftui.ImGuiUtil;
import com.mojang.blaze3d.systems.RenderSystem;

import imgui.ImFont;
import imgui.ImFontAtlas;
import imgui.ImGui;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

public class ImFontManager implements IdentifiableResourceReloadListener {

    private static ImFontManager instance;

    public static ImFontManager getInstance() {
        if (instance == null) {
            instance = new ImFontManager();
        }
        return instance;
    }

    private ImFontManager() {};

    private static ImFontAtlas getFontAtlas() {
        return ImGui.getIO().getFonts();
    }

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final Map<Identifier, ImFont> fonts = new HashMap<>();
    private ImFont defaultFont;

    // Keep track of the missing fonts that have already been complained about in
    // console so we don't get spam every frame.
    private final Set<Identifier> complainedIds = new HashSet<>();

    @Override
    public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager, Profiler prepareProfiler, Profiler applyProfiler,
            Executor prepareExecutor, Executor applyExecuytor) {
        
        Map<Identifier, byte[]> buffers = new ConcurrentHashMap<>();
        Map<Identifier, Resource> resources = manager.findResources("fonts", i -> i.getPath().endsWith(".ttf"));
            
        List<CompletableFuture<?>> futures = new ArrayList<>(resources.size());
        for (var entry : resources.entrySet()) {
            futures.add(CompletableFuture.runAsync(() -> {

                String trimmedPath = entry.getKey().getPath();
                trimmedPath = trimmedPath.substring("fonts/".length());
                trimmedPath = FilenameUtils.removeExtension(trimmedPath);
                Identifier trimmedId = new Identifier(entry.getKey().getNamespace(), trimmedPath);
                LOGGER.info("Loading font " + entry.getKey() + " as " + trimmedId);

                try(InputStream in = new BufferedInputStream(entry.getValue().getInputStream())) {
                    buffers.put(trimmedId, in.readAllBytes());
                } catch (IOException e) {
                    LOGGER.error("Error reading " + entry.getKey(), e);
                }

            }, prepareExecutor));    
        }
        
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenCompose(synchronizer::whenPrepared)
                .thenRunAsync(() -> loadFonts(buffers), applyExecuytor);
    }

    // TODO: make this configurable
    private void loadFonts(Map<Identifier, byte[]> buffers) {
        RenderSystem.assertOnRenderThreadOrInit();
        ImGuiUtil.ensureInitialized();

        fonts.clear();
        complainedIds.clear();

        ImFontAtlas atlas = getFontAtlas();
        atlas.clear();
        
        defaultFont = atlas.addFontDefault();

        for (var entry : buffers.entrySet()) {
            ImFont font = atlas.addFontFromMemoryTTF(entry.getValue(), 16);
            fonts.put(entry.getKey(), font);
        }
        atlas.build();
        ImGuiUtil.IM_GL3.updateFontsTexture();
        atlas.clearTexData();

        LOGGER.info("Created font atlas with " + fonts.size() + " font(s)");
    }


    /**
     * Get all the loaded fonts.
     * @return An unmodifiable map of fonts.
     */
    public Map<Identifier, ImFont> getFonts() {
        return Collections.unmodifiableMap(fonts);
    }
    
    /**
     * Get a font by its ID.
     * @param id ID to use.
     * @return The font, or a default font if it does not exist.
     */
    public ImFont get(Identifier id) {
        ImFont font = fonts.get(id);
        if (font == null) {
            if (!complainedIds.contains(id)) {
                LOGGER.warn("Unknown font: " + id);
                complainedIds.add(id);
            }
            return defaultFont;
        }
        return font;
    }

    /**
     * Get a font by its ID. Shortcut for <code>getInstance().get(id)</code>.
     * @param id ID to use.
     * @return The font, or a default font if it does not exist.
     */
    public static ImFont getFont(Identifier id) {
        return getInstance().get(id);
    }

    @Override
    public Identifier getFabricId() {
        return new Identifier("craftui:fonts");
    }
    
    public static class FontSubDefinition {
        /**
         * Base sizse of the font in pixels
         */
        public float size;

        public Identifier name;

        @Nullable
        public Integer oversampleH;

        @Nullable
        public Integer oversampleV;

        @Nullable
        public Boolean pixelSnapH;

        @Nullable
        public Vector2f glyphExtraSpacing;

        @Nullable
        public Vector2f glyphOffset;

        public final List<GlyphRange> glyphRanges = new ArrayList<>();

        @Nullable
        public Float glyphMinAdvanceX;
        
    }

    @JsonAdapter(GlyphRangeJsonAdapter.class)
    public static record GlyphRange(short min, short max) {};

    private static class GlyphRangeJsonAdapter implements JsonSerializer<GlyphRange>, JsonDeserializer<GlyphRange> {

        @Override
        public GlyphRange deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonArray array = json.getAsJsonArray();
            if (array.size() != 2) {
                throw new JsonParseException("Improper array size (%d) != 2".formatted(array.size()));
            }
            JsonPrimitive first = array.get(0).getAsJsonPrimitive();
            JsonPrimitive second = array.get(1).getAsJsonPrimitive();

            short min;
            short max;

            if (first.isString()) {
                min = Short.parseShort(first.getAsString(), 16);
                max = Short.parseShort(second.getAsString(), 16);
            } else {
                min = first.getAsShort();
                max = second.getAsShort();
            }

            return new GlyphRange(min, max);
        }

        @Override
        public JsonElement serialize(GlyphRange src, Type typeOfSrc, JsonSerializationContext context) {
            JsonArray array = new JsonArray(2);
            array.add(Integer.toHexString(src.min));
            array.add(Integer.toHexString(src.max));
            return array;
        }
        
    }
}

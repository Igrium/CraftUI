package com.igrium.craftui.impl.font;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.igrium.craftui.event.FontReloadCallback;
import com.igrium.craftui.impl.util.IdentifierJsonAdapter;
import com.igrium.craftui.impl.render.ImGuiUtil;
import com.igrium.craftui.impl.util.Vector2fJsonAdapter;
import com.mojang.blaze3d.systems.RenderSystem;

import imgui.ImFont;
import imgui.ImFontAtlas;
import imgui.ImFontConfig;
import imgui.ImGui;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

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
    private final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Identifier.class, new IdentifierJsonAdapter())
            .registerTypeAdapter(Vector2f.class, new Vector2fJsonAdapter())
            .setPrettyPrinting()
            .create();

    private final Map<Identifier, ImFont> fonts = new HashMap<>();
    private ImFont defaultFont;

    // Store byte data of font files for fast scale changes
    private final Map<Identifier, LoadedFontFile> fontFiles = new HashMap<>();

    // Keep track of the missing fonts that have already been complained about in
    // console so we don't get spam every frame.
    private final Set<Identifier> complainedIds = new HashSet<>();

    @Override
    public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager, Executor prepareExecutor, Executor applyExecutor) {
        
        fontFiles.clear();
        
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (var entry : manager.findResources("fonts", id -> id.getPath().endsWith(".ttf")).entrySet()) {
            /* Calculate font ID and create font file entry */
            final Identifier fileName = entry.getKey();

            String fontPath = fileName.getPath().substring("fonts/".length());
            fontPath = FilenameUtils.removeExtension(fontPath);
            final Identifier fontID = Identifier.of(fileName.getNamespace(), fontPath);
            
            final LoadedFontFile file = new LoadedFontFile();
            fontFiles.put(fontID, file);

            // async
            futures.add(CompletableFuture.runAsync(() -> {

                /* Find and load config */
                Identifier configId = Identifier.of(fontID.getNamespace(), "fonts/" + fontID.getPath() + ".json");
                Optional<Resource> configFile = manager.getResource(configId);
                FontConfig config = new FontConfig();

                if (configFile.isPresent()) {
                    try(BufferedReader reader = configFile.get().getReader()) {
                        config = GSON.fromJson(reader, FontConfig.class);
                    } catch (Exception e) {
                        // If the config errors, we still have the default config.
                        LOGGER.error("Error loading font config file " + configId, e);
                    }
                }

                file.config = config;

                /* Load the font file contents itself */
                LOGGER.info("Loading font {} as {}", fileName, fontID);
                try(InputStream in = new BufferedInputStream(entry.getValue().getInputStream())) {
                    file.fileContents = in.readAllBytes();
                } catch (Exception e) {
                    LOGGER.error("Error loading font " + fontID, e);
                }

            }, prepareExecutor));
        }

        // After all the above futures are done, fontFiles should be populated with all
        // loaded fonts.
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenCompose(synchronizer::whenPrepared)
                .thenRunAsync(this::renderFonts, applyExecutor);

    }

    /**
     * Re-render all loaded fonts. Call this when UI size changes.
     */
    public void renderFonts() {
        renderFonts(this.fontFiles);
    }

    private void renderFonts(Map<Identifier, LoadedFontFile> files) {
        RenderSystem.assertOnRenderThreadOrInit();
        ImGuiUtil.ensureInitialized();

        fonts.clear();
        complainedIds.clear();

        ImFontAtlas atlas = getFontAtlas();
        atlas.clear();
        
        defaultFont = atlas.addFontDefault();

        for (var entry : files.entrySet()) {
            LoadedFontFile file = entry.getValue();
            if (file.fileContents == null || file.fileContents.length == 0) {
                LOGGER.warn("Font {} did not have any file contents. Check above for errors.", entry.getKey());
                continue;
            }

            try {
                ImFont font = renderFont(atlas, file);
                fonts.put(entry.getKey(), font);
            } catch (Exception e) {
                LOGGER.error("Error rendering font {}. The ttf file was likely invalid.",entry.getKey(), e);
            }
        }
        atlas.build();
        ImGuiUtil.IM_GL3.updateFontsTexture();
        atlas.clearTexData();

        LOGGER.info("Created font atlas with {} font(s)", fonts.size());
        FontReloadCallback.EVENT.invoker().onFontsReloaded(this);
    }

    private ImFont renderFont(ImFontAtlas atlas, LoadedFontFile file) {
        FontConfig config = file.config;
        ImFontConfig imConfig = new ImFontConfig();
        float size = config.size * 16; // TODO: UI scaling
        try {
            imConfig.setSizePixels(size);
            if (config.oversampleH != null)
                imConfig.setOversampleH(config.oversampleH);
            if (config.oversampleV != null)
                imConfig.setOversampleV(config.oversampleV);
            if (config.pixelSnapH != null)
                imConfig.setPixelSnapH(config.pixelSnapH);
            if (config.glyphMinAdvanceX != null)
                imConfig.setGlyphMinAdvanceX(config.glyphMinAdvanceX);
            if (config.glyphMaxAdvanceX != null)
                imConfig.setGlyphMaxAdvanceX(config.glyphMaxAdvanceX);

            if (config.glyphExtraSpacing != null)
                imConfig.setGlyphExtraSpacing(
                        config.glyphExtraSpacing.getX(),
                        config.glyphExtraSpacing.getY());

            if (config.glyphOffset != null || config.scaledGlyphOffset != null) {
                // TODO: make this the actual scale
                float scale = 1;

                float offsetX = 0;
                float offsetY = 0;

                if (config.glyphOffset != null) {
                    offsetX += config.glyphOffset.getX();
                    offsetY += config.glyphOffset.getY();
                }
                if (config.scaledGlyphOffset != null) {
                    offsetX += config.scaledGlyphOffset.getX() * scale;
                    offsetY += config.scaledGlyphOffset.getY() * scale;
                }

                imConfig.setGlyphOffset(offsetX, offsetY);
            }

            
            return atlas.addFontFromMemoryTTF(file.fileContents, size, imConfig);
        } finally {
            imConfig.destroy();
        }
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

    @Override
    public Identifier getFabricId() {
        return Identifier.of("craftui:fonts");
    }

    private static class FontConfig {
        /**
         * Base size of the font in pixels
         */
        public float size = 1f;

        @Nullable
        public Integer oversampleH;

        @Nullable
        public Integer oversampleV;

        @Nullable
        public Boolean pixelSnapH;

        @Nullable
        public Float glyphMinAdvanceX;

        @Nullable
        public Float glyphMaxAdvanceX;

        @Nullable
        public Vector2f glyphExtraSpacing;

        @Nullable
        public Vector2f glyphOffset;

        @Nullable
        public Vector2f scaledGlyphOffset;
    }

    private static class LoadedFontFile {
        FontConfig config;
        byte[] fileContents;
    }
}

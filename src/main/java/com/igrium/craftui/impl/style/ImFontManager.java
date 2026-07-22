package com.igrium.craftui.impl.style;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
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
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class ImFontManager implements IdentifiableResourceReloadListener {

    private static ImFontManager instance;

    public static ImFontManager getInstance() {
        if (instance == null) {
            instance = new ImFontManager();
        }
        return instance;
    }

    private ImFontManager() {}

    private static ImFontAtlas getFontAtlas() {
        return ImGui.getIO().getFonts();
    }

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());
    private final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Identifier.class, new IdentifierJsonAdapter())
            .registerTypeAdapter(UVPair.class, new Vector2fJsonAdapter())
            .setPrettyPrinting()
            .create();

    private final Map<Identifier, ImFont> fonts = new HashMap<>();
    private ImFont defaultFont;

    // Store byte data of font files for fast scale changes
    private final Map<Identifier, LoadedFontFile> fontFiles = new HashMap<>();

    // Keep track of the missing fonts that have already been complained about in
    // console so we don't get spam every frame.
    private final Set<Identifier> complainedIds = new HashSet<>();

    private static boolean isFontExt(String path) {
        return path.endsWith(".ttf") || path.endsWith(".otf");
    }

    @Override
    public CompletableFuture<Void> reload(SharedState currentReload, Executor prepareExecutor, PreparationBarrier synchronizer, Executor applyExecutor) {
        ResourceManager manager = currentReload.resourceManager();

        fontFiles.clear();

        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (var entry : manager.listResources("fonts", id -> isFontExt(id.getPath())).entrySet()) {
            /* Calculate font ID and create font file entry */
            final Identifier fileName = entry.getKey();

            String fontPath = fileName.getPath().substring("fonts/".length());
            fontPath = FilenameUtils.removeExtension(fontPath);
            final Identifier fontID = Identifier.fromNamespaceAndPath(fileName.getNamespace(), fontPath);

            final LoadedFontFile file = new LoadedFontFile();
            fontFiles.put(fontID, file);

            // async
            futures.add(CompletableFuture.runAsync(() -> {

                /* Find and load config */
                Identifier configId = Identifier.fromNamespaceAndPath(fontID.getNamespace(), "fonts/" + fontID.getPath() + ".json");
                Optional<Resource> configFile = manager.getResource(configId);
                FontConfig config = new FontConfig();

                if (configFile.isPresent()) {
                    try(BufferedReader reader = configFile.get().openAsReader()) {
                        config = GSON.fromJson(reader, FontConfig.class);
                    } catch (Exception e) {
                        // If the config errors, we still have the default config.
                        LOGGER.error("Error loading font config file " + configId, e);
                    }
                }

                file.config = config;

                /* Load the font file contents itself */
                LOGGER.info("Loading font {} as {}", fileName, fontID);
                try(InputStream in = new BufferedInputStream(entry.getValue().open())) {
                    file.fileContents = in.readAllBytes();
                } catch (Exception e) {
                    LOGGER.error("Error loading font " + fontID, e);
                }

            }, prepareExecutor));
        }

        // After all the above futures are done, fontFiles should be populated with all
        // loaded fonts.
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenCompose(synchronizer::wait)
                .thenRunAsync(this::renderFonts, applyExecutor);
    }

    /**
     * Re-render all loaded fonts. Call this when UI size changes.
     */
    public void renderFonts() {
        renderFonts(this.fontFiles);
    }

    private void renderFonts(Map<Identifier, LoadedFontFile> files) {
        RenderSystem.assertOnRenderThread();
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
                if (entry.getValue().config.iconOnly) {
                    continue;
                }
                ImFont font = renderFont(atlas, file, entry.getKey(), false, files);
                fonts.put(entry.getKey(), font);
            } catch (Exception e) {
                LOGGER.error("Error rendering font {}. The ttf/otf file was likely invalid.", entry.getKey(), e);
            }
        }
        atlas.build();
        ImGuiUtil.IM_BLAZE3D.createFontsTexture();
        atlas.clearTexData();

        var fontIterator = fonts.entrySet().iterator();
        while (fontIterator.hasNext()) {
            var entry = fontIterator.next();
            if (!entry.getValue().isLoaded()) {
                LOGGER.warn("Font {} was not loaded properly. Please ensure valid glyph ranges.", entry.getKey());
                fontIterator.remove();
            }
        }

        LOGGER.info("Created font atlas with {} font(s)", fonts.size());
        FontReloadCallback.EVENT.invoker().onFontsReloaded(this);
    }

    /**
     * Render a font from memory onto the GPU.
     *
     * @param atlas Font atlas to render into.
     * @param file  Font file to render.
     * @param id    The ID of the font we're rendering.
     * @param merge Indicates that this font is being appended to a "parent" font.
     * @param fonts Map of all fonts being loaded (used to locate icon fonts).
     * @return The ImFont
     */
    private ImFont renderFont(ImFontAtlas atlas, LoadedFontFile file, Identifier id,
                              boolean merge, Map<Identifier, LoadedFontFile> fonts) {
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

            if (config.glyphOffset != null || config.scaledGlyphOffset != null) {
                float scale = 1;

                float offsetX = 0;
                float offsetY = 0;

                if (config.glyphOffset != null) {
                    offsetX += config.glyphOffset.u();
                    offsetY += config.glyphOffset.v();
                }
                if (merge && config.iconGlyphOffset != null) {
                    offsetX += config.iconGlyphOffset.u() * scale;
                    offsetY += config.iconGlyphOffset.v() * scale;
                } else if (config.scaledGlyphOffset != null) {
                    offsetX += config.scaledGlyphOffset.u() * scale;
                    offsetY += config.scaledGlyphOffset.v() * scale;
                }

                imConfig.setGlyphOffset(offsetX, offsetY);
            }

            imConfig.setName(id.toString());

            if (merge) {
                imConfig.setMergeMode(true);
                if (config.glyphRanges == null) {
                    LOGGER.warn("Tried to load {} as an icon font, but no glyph ranges are specified!", id);
                }
            }

            if (config.glyphExcludeRanges != null) {
                imConfig.setGlyphExcludeRanges(zeroTerminate(config.glyphExcludeRanges));
            }

            ImFont font;
            if (config.glyphRanges != null) {
                font = atlas.addFontFromMemoryTTF(file.fileContents, size, imConfig, zeroTerminate(config.glyphRanges));
            } else {
                font = atlas.addFontFromMemoryTTF(file.fileContents, size, imConfig);
            }

            if (!merge) {
                for (var iconId : config.icons) {
                    LoadedFontFile icon = fonts.get(iconId);
                    if (icon != null) {
                        renderFont(atlas, icon, iconId, true, fonts);
                    } else {
                        LOGGER.warn("Unable to locate icon font {}", iconId);
                    }
                }
            }

            return font;
        } finally {
            imConfig.destroy();
        }
    }

    /**
     * Append a zero terminator to a glyph range array, as required by imgui
     */
    private static short[] zeroTerminate(short[] ranges) {
        if (ranges.length > 0 && ranges[ranges.length - 1] != 0) {
            return Arrays.copyOf(ranges, ranges.length + 1);
        } else {
            return ranges;
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
        return Identifier.parse("craftui:fonts");
    }

    private static class FontConfig {
        float size = 1f;

        @Nullable Integer oversampleH;

        @Nullable Integer oversampleV;

        Boolean pixelSnapH;

        @Nullable Float glyphMinAdvanceX;

        @Nullable Float glyphMaxAdvanceX;

        @Nullable UVPair glyphOffset;

        @Nullable UVPair scaledGlyphOffset;

        /**
         * The glyph offset to use when loading this as an icon font.
         * Overrides <code>scaledGlyphOffset</code>
         */
        @Nullable UVPair iconGlyphOffset;

        @JsonAdapter(GlyphRangeTypeAdapter.class)
        short @Nullable [] glyphRanges;

        @JsonAdapter(GlyphRangeTypeAdapter.class)
        short @Nullable [] glyphExcludeRanges;

        /**
         * The icon fonts to load on top of this one.
         */
        Identifier[] icons = new Identifier[0];

        boolean iconOnly = false;
    }


    private static class GlyphRangeTypeAdapter extends TypeAdapter<short[]> {

        @Override
        public void write(JsonWriter out, short[] value) throws IOException {
            if (value == null) {
                out.nullValue();
                return;
            }
            out.beginArray();
            for (int i : value) {
                out.value(Integer.toHexString(i));
            }
            out.endArray();
        }

        @Override
        public short[] read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            ShortList list = new ShortArrayList();
            in.beginArray();
            while (in.peek() != JsonToken.END_ARRAY) {
                if (in.peek() == JsonToken.STRING) {
                    list.add((short) parseInt(in.nextString()));
                } else {
                    list.add((short) in.nextInt());
                }
            }
            in.endArray();
            return list.toShortArray();
        }

        private static int parseInt(String hex) {
            if (hex.startsWith("0x") || hex.startsWith("0X")) {
                hex = hex.substring(2);
            } else if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            // Parse using a Long to bypass signed bit limitations on high hex values before casting to int
            return (int) Long.parseLong(hex, 16);
        }
    }

    private static class LoadedFontFile {
        FontConfig config;
        byte[] fileContents;
    }
}
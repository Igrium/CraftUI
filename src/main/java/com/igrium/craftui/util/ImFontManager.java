package com.igrium.craftui.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.mojang.blaze3d.systems.RenderSystem;

import imgui.ImFont;
import imgui.ImGui;
import imgui.ImGuiIO;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

/**
 * Deals with loading & caching ImGui fonts from resourcepacks.
 */
public class ImFontManager implements IdentifiableResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImFontManager.class);
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Identifier.class, new GsonAdapters.IdentifierJsonAdapter())
            .setPrettyPrinting()
            .create();
    
    private static ImFontManager instance;

    private final BiMap<Identifier, ImFont> fonts = HashBiMap.create();
    private boolean initialized;
    private final Map<Identifier, FontFile> fontFiles = new HashMap<>();
    
    private ImFont defaultFont;

    public ImFontManager() {
        instance = this;
    }

    public static ImFontManager getInstance() {
        return instance;
    }

    @Override
    public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager, Profiler prepareProfiler,
            Profiler applyProfiler, Executor prepareExecutor, Executor applyExecutor) {

        // TypeToken<Map<Identifier, List<FontDeclaration>>> token = new TypeToken<>() {};
        // Map<Identifier, List<FontDeclaration>> fonts = new HashMap<>();
        TypeToken<Map<Identifier, FontDeclaration>> token = new TypeToken<>() {};
        Map<Identifier, FontDeclaration> fonts = new HashMap<>();
        
        fontFiles.clear();
        initialized = false;

        // Compile all json files into font definitions
        for (var resource : manager.getAllResources(new Identifier("craftui:font/fonts.json"))) {
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                var loaded = GSON.fromJson(reader, token);
                fonts.putAll(loaded);
            } catch (IOException e) {
                LOGGER.error("Error reading fonts.json", e);
            }
        }

        if (fonts.isEmpty()) {
            synchronizer.whenPrepared(null);
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<?>> futures = new ArrayList<>(fonts.size());

        // Load fonts from file
        for (var entry : fonts.entrySet()) {
            FontFile file = new FontFile(entry.getValue());
            fontFiles.put(entry.getKey(), file);
            futures.add(CompletableFuture.runAsync(() -> readFromDisk(file, manager), prepareExecutor)
                    .thenCompose(synchronizer::whenPrepared));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    // Yeah I know this approach may end up with file duplicates in memory. They're
    // really small so I don't care.
    private void readFromDisk(FontFile file, ResourceManager resourceManager) {
        var opt = resourceManager.getResource(file.declaration.font);
        if (!opt.isPresent()) {
            LOGGER.warn("Missing font file: " + file.declaration.font);
            return;
        }

        try(InputStream in = new BufferedInputStream(opt.get().getInputStream())) {
            file.contents = in.readAllBytes();

        } catch (IOException e) {
            LOGGER.error("Error loading font file " + file.declaration.font, e);
        }
        LOGGER.info("Read font data from " + file.declaration.font);
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Parse TTF font data and load onto GPU
     */
    public void initFonts() {
        if (initialized)
            return;

        RenderSystem.assertOnRenderThreadOrInit();
        fonts.clear();

        ImGuiIO io = ImGui.getIO();
        for (var entry : fontFiles.entrySet()) {
            FontFile file = entry.getValue();
            FontDeclaration def = file.declaration;
            if (file.contents == null || file.contents.length == 0) {
                LOGGER.warn("No data was loaded for font " + entry.getKey());
                continue;
            }

            ImFont font = io.getFonts().addFontFromMemoryTTF(file.contents, def.size);
            fonts.put(entry.getKey(), font);
        }
        defaultFont = io.getFonts().addFontDefault();
        fonts.put(new Identifier("craftui:default"), defaultFont);
        io.getFonts().build();
        initialized = true;

    }

    @Override
    public Identifier getFabricId() {
        return new Identifier("craftui:fonts");
    }

    public Map<Identifier, ImFont> getFonts() {
        return Collections.unmodifiableMap(fonts);
    }

    public ImFont get(Identifier id) {
        return fonts.getOrDefault(id, defaultFont);
    }

    public static ImFont getFont(Identifier id) {
        return instance.get(id);
    }

    // TODO: add more options
    @SuppressWarnings("unused") // WIP
    private static class FontDeclaration {
        public Identifier font = new Identifier("craftui:default");

        public float size = 16;

        public boolean monospace;

        public Hexable[] range = new Hexable[2];

        @Override
        public String toString() {
            return GSON.toJson(this);
        }
    }
    
    private static class FontFile {
        public FontFile(FontDeclaration declaration) {
            this.declaration = declaration;
        }

        public final FontDeclaration declaration;
        public byte[] contents = null;
        
        @Override
        public String toString() {
            return GSON.toJson(this);
        }
    }

    @JsonAdapter(HexTypeAdapter.class)
    private static class Hexable extends Number {

        private final int value;

        private Hexable(int value) {
            this.value = value;
        }

        @Override
        public double doubleValue() {
            return value;
        }

        @Override
        public float floatValue() {
            return value;
        }

        @Override
        public int intValue() {
            return value;
        }

        @Override
        public long longValue() {
            return value;
        }

        public String hexValue() {
            return Integer.toHexString(value);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Number number) {
                return this.value == number.intValue();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        public String toString() {
            return "#" + hexValue();
        }

        public static Hexable fromInt(int value) {
            return new Hexable(value);
        }

        public static Hexable fromHex(String hex) {
            return new Hexable(Integer.parseInt(hex, 16));
        }
    }

    private static class HexTypeAdapter extends TypeAdapter<Hexable> {

        @Override
        public void write(JsonWriter out, Hexable value) throws IOException {
            out.value(value.hexValue());
        }

        @Override
        public Hexable read(JsonReader in) throws IOException {
            JsonToken token = in.peek();
            if (token == JsonToken.STRING) {
                return Hexable.fromHex(in.nextString());
            } else {
                return Hexable.fromInt(in.nextInt());
            }
        }
        
    }

    // private static class HexTypeAdapter implements JsonSerializer<int[]>, JsonDeserializer<int[]> {

    //     @Override
    //     public int[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
    //             throws JsonParseException {
    //         JsonArray jArray = json.getAsJsonArray();
    //         if (jArray.isEmpty()) {
    //             return new int[0];
    //         }
            
    //         int[] array = new int[jArray.size()];
    //         JsonPrimitive first = jArray.get(0).getAsJsonPrimitive();
    //         if (first.isString()) {
    //             for (int i = 0; i < array.length; i++) {
    //                 array[i] = Integer.parseInt(jArray.get(i).getAsString());
    //             }
    //         } else if (first.isNumber()) {
    //             for (int i = 0; i < array.length; i++) {
    //                 array[i] = jArray.get(i).getAsInt();
    //             }
    //         } else {
    //             throw new JsonParseException("Umproper json value: " + first);
    //         }
            
    //         return array;
    //     }

    //     @Override
    //     public JsonElement serialize(int[] src, Type typeOfSrc, JsonSerializationContext context) {
    //         JsonArray array = new JsonArray(src.length);
    //         for (int val : src) {
    //             array.add(Integer.toHexString(val));
    //         }
    //         return array;
    //     }

        
    // }
}

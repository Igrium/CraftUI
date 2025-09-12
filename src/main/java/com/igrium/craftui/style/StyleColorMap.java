package com.igrium.craftui.style;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import imgui.ImColor;

import imgui.ImGuiStyle;
import imgui.ImVec4;
import imgui.flag.ImGuiCol;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the color portion of a CraftUI style.
 * Contains "advanced" methods to establish color definitions so you're not duplicating a bunch of colors.
 */
@Getter
public class StyleColorMap {

    /**
     * An IMGUI color definition.
     * @param color Raw int value of the color. Unused if colorDef is set.
     * @param ref Indicates that this color references something in the global map.
     */
    @JsonAdapter(ColorRefJsonAdapter.class)
    private record ColorRef(int color, @Nullable String ref) {
        static final ColorRef BLACK = new ColorRef(0, null);

        int get(Map<String, ColorRef> defs) {
            if (ref != null) {
                ColorRef r2 = defs.get(ref);
                if (r2 != null) {
                    return r2.color;
                }
            }
            return color;
        }
    }

    private static class ColorRefJsonAdapter extends TypeAdapter<ColorRef> {

        @Override
        public void write(JsonWriter out, ColorRef value) throws IOException {
            if (value.ref() != null) {
                out.value(value.ref());
            } else {
                String str = Integer.toHexString(Integer.reverseBytes(value.color));
                out.value("#" + str);
            }
        }

        @Override
        public ColorRef read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.STRING) {
                String str = in.nextString();
                if (str.startsWith("#")) {
                    if (str.length() == 7) {
                        str += "FF"; // hack to make full alpha if the user forgot.
                    }
                    int val = Integer.reverseBytes(Integer.parseUnsignedInt(str.substring(1), 16));
                    return new ColorRef(val, null);
                } else {
                    return new ColorRef(0, str);
                }
            } else {
                return new ColorRef(readColor(in), null);
            }
        }
    }


    private final Map<String, ColorRef> defs = new HashMap<>();

    private final Map<String, ColorRef> colors = new HashMap<>();

    public void appendAll(StyleColorMap other) {
        defs.putAll(other.defs);
        colors.putAll(other.colors);
    }

    public void buildStyle(ImGuiStyle style) {
        for (var entry : colors.entrySet()) {
            int color = entry.getValue().get(defs);
            Integer colorId = colorNames.get(entry.getKey());
            if (colorId == null) {
                LoggerFactory.getLogger(getClass()).warn("Unknown IMGUI color: " + entry.getKey());
            } else {
                style.setColor(colorId, color);
            }
        }
    }

    public void fromStyle(ImGuiStyle style) {
        ImVec4 col = new ImVec4();
        colors.clear();
        defs.clear();
        for (var entry : colorNames.entrySet()) {
            style.getColor(entry.getValue(), col);
            colors.put(entry.getKey(), new ColorRef(ImColor.rgba(col), null));
        }
    }


    private static int readColor(JsonReader reader) throws IOException {
        switch(reader.peek()) {
            case NUMBER -> {
                return reader.nextInt();
            } case BEGIN_ARRAY -> {
                float[] vals = new float[4];
                int i = 0;
                reader.beginArray();
                while (reader.peek() != JsonToken.END_ARRAY && i < 4) {
                    vals[i] = (float) reader.nextDouble();
                    i++;
                }
                reader.endArray();

                // Ensure full alpha if it was omitted.
                if (i < 4) {
                    vals[3] = 1;
                }
                return ImColor.rgba(vals[0], vals[1], vals[2], vals[3]);
            } default -> throw new JsonParseException("Invalid json token type for color.");
        }
    }


    private static final BiMap<String, Integer> colorNames = HashBiMap.create();

    static {
        colorNames.put("text", ImGuiCol.Text);
        colorNames.put("textDisabled", ImGuiCol.TextDisabled);
        colorNames.put("windowBg", ImGuiCol.WindowBg);
        colorNames.put("childBg", ImGuiCol.ChildBg);
        colorNames.put("popupBg", ImGuiCol.PopupBg);
        colorNames.put("border", ImGuiCol.Border);
        colorNames.put("borderShadow", ImGuiCol.BorderShadow);
        colorNames.put("frameBg", ImGuiCol.FrameBg);
        colorNames.put("frameBgHovered", ImGuiCol.FrameBgHovered);
        colorNames.put("frameBgActive", ImGuiCol.FrameBgActive);
        colorNames.put("titleBg", ImGuiCol.TitleBg);
        colorNames.put("titleBgActive", ImGuiCol.TitleBgActive);
        colorNames.put("titleBgCollapsed", ImGuiCol.TitleBgCollapsed);
        colorNames.put("menuBarBg", ImGuiCol.MenuBarBg);
        colorNames.put("scrollbarBg", ImGuiCol.ScrollbarBg);
        colorNames.put("scrollbarGrab", ImGuiCol.ScrollbarGrab);
        colorNames.put("scrollbarGrabHovered", ImGuiCol.ScrollbarGrabHovered);
        colorNames.put("scrollbarGrabActive", ImGuiCol.ScrollbarGrabActive);
        colorNames.put("checkMark", ImGuiCol.CheckMark);
        colorNames.put("sliderGrab", ImGuiCol.SliderGrab);
        colorNames.put("sliderGrabActive", ImGuiCol.SliderGrabActive);
        colorNames.put("button", ImGuiCol.Button);
        colorNames.put("buttonHovered", ImGuiCol.ButtonHovered);
        colorNames.put("buttonActive", ImGuiCol.ButtonActive);
        colorNames.put("header", ImGuiCol.Header);
        colorNames.put("headerHovered", ImGuiCol.HeaderHovered);
        colorNames.put("headerActive", ImGuiCol.HeaderActive);
        colorNames.put("separator", ImGuiCol.Separator);
        colorNames.put("separatorHovered", ImGuiCol.SeparatorHovered);
        colorNames.put("separatorActive", ImGuiCol.SeparatorActive);
        colorNames.put("resizeGrip", ImGuiCol.ResizeGrip);
        colorNames.put("resizeGripHovered", ImGuiCol.ResizeGripHovered);
        colorNames.put("resizeGripActive", ImGuiCol.ResizeGripActive);
        colorNames.put("tab", ImGuiCol.Tab);
        colorNames.put("tabHovered", ImGuiCol.TabHovered);
        colorNames.put("tabActive", ImGuiCol.TabActive);
        colorNames.put("tabUnfocused", ImGuiCol.TabUnfocused);
        colorNames.put("tabUnfocusedActive", ImGuiCol.TabUnfocusedActive);
        colorNames.put("dockingPreview", ImGuiCol.DockingPreview);
        colorNames.put("dockingEmptyBg", ImGuiCol.DockingEmptyBg);
        colorNames.put("plotLines", ImGuiCol.PlotLines);
        colorNames.put("plotLinesHovered", ImGuiCol.PlotLinesHovered);
        colorNames.put("plotHistogram", ImGuiCol.PlotHistogram);
        colorNames.put("plotHistogramHovered", ImGuiCol.PlotHistogramHovered);
        colorNames.put("tableHeaderBg", ImGuiCol.TableHeaderBg);
        colorNames.put("tableBorderStrong", ImGuiCol.TableBorderStrong);
        colorNames.put("tableBorderLight", ImGuiCol.TableBorderLight);
        colorNames.put("tableRowBg", ImGuiCol.TableRowBg);
        colorNames.put("tableRowBgAlt", ImGuiCol.TableRowBgAlt);
        colorNames.put("textSelectedBg", ImGuiCol.TextSelectedBg);
        colorNames.put("dragDropTarget", ImGuiCol.DragDropTarget);
        colorNames.put("navHighlight", ImGuiCol.NavHighlight);
        colorNames.put("navWindowingHighlight", ImGuiCol.NavWindowingHighlight);
        colorNames.put("navWindowingDimBg", ImGuiCol.NavWindowingDimBg);
        colorNames.put("modalWindowDimBg", ImGuiCol.ModalWindowDimBg);
    }
}
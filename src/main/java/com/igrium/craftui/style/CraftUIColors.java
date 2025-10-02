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
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the color portion of a CraftUI style.
 * Contains "advanced" methods to establish color definitions so you're not duplicating a bunch of colors.
 */
@Getter @UtilityClass
public class CraftUIColors {

    /**
     * An IMGUI color definition.
     * @param color Raw int value of the color. Unused if colorDef is set.
     * @param ref Indicates that this color references something in the global map.
     */
    @JsonAdapter(UIColorJsonAdapter.class)
    public record UIColor(int color, @Nullable String ref) {
        static final UIColor BLACK = new UIColor(0, null);

        int get(Map<String, UIColor> defs) {
            if (ref != null) {
                UIColor r2 = defs.get(ref);
                if (r2 != null) {
                    return r2.color;
                }
            }
            return color;
        }
    }

    private static class UIColorJsonAdapter extends TypeAdapter<UIColor> {

        @Override
        public void write(JsonWriter out, UIColor value) throws IOException {
            if (value.ref() != null) {
                out.value(value.ref());
            } else {
                String str = Integer.toHexString(Integer.reverseBytes(value.color));
                out.value("#" + str);
            }
        }

        @Override
        public UIColor read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.STRING) {
                String str = in.nextString();
                if (str.startsWith("#")) {
                    if (str.length() == 7) {
                        str += "FF"; // hack to make full alpha if the user forgot.
                    }
                    int val = Integer.reverseBytes(Integer.parseUnsignedInt(str.substring(1), 16));
                    return new UIColor(val, null);
                } else {
                    return new UIColor(0, str);
                }
            } else {
                return new UIColor(readColor(in), null);
            }
        }
    }


    public void buildStyle(ImGuiStyle style, Map<? extends String, ? extends UIColor> colors) {
        for (var entry : colors.entrySet()) {
            int color = entry.getValue().color();
            Integer colorId = COLOR_NAMES.get(entry.getKey());
            if (colorId == null) {
                LoggerFactory.getLogger(CraftUIColors.class).warn("Unknown IMGUI color: " + entry.getKey());
            } else {
                style.setColor(colorId, color);
            }
        }
    }

    public Map<String, UIColor> fromStyle(ImGuiStyle style) {
        ImVec4 col = new ImVec4();
        Map<String, UIColor> colors = new HashMap<>();
        for (var entry : COLOR_NAMES.entrySet()) {
            style.getColor(entry.getValue(), col);
            colors.put(entry.getKey(), new UIColor(ImColor.rgba(col), null));
        }
        return colors;
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


    public static final BiMap<String, Integer> COLOR_NAMES = HashBiMap.create();

    static {
        COLOR_NAMES.put("text", ImGuiCol.Text);
        COLOR_NAMES.put("textDisabled", ImGuiCol.TextDisabled);
        COLOR_NAMES.put("windowBg", ImGuiCol.WindowBg);
        COLOR_NAMES.put("childBg", ImGuiCol.ChildBg);
        COLOR_NAMES.put("popupBg", ImGuiCol.PopupBg);
        COLOR_NAMES.put("border", ImGuiCol.Border);
        COLOR_NAMES.put("borderShadow", ImGuiCol.BorderShadow);
        COLOR_NAMES.put("frameBg", ImGuiCol.FrameBg);
        COLOR_NAMES.put("frameBgHovered", ImGuiCol.FrameBgHovered);
        COLOR_NAMES.put("frameBgActive", ImGuiCol.FrameBgActive);
        COLOR_NAMES.put("titleBg", ImGuiCol.TitleBg);
        COLOR_NAMES.put("titleBgActive", ImGuiCol.TitleBgActive);
        COLOR_NAMES.put("titleBgCollapsed", ImGuiCol.TitleBgCollapsed);
        COLOR_NAMES.put("menuBarBg", ImGuiCol.MenuBarBg);
        COLOR_NAMES.put("scrollbarBg", ImGuiCol.ScrollbarBg);
        COLOR_NAMES.put("scrollbarGrab", ImGuiCol.ScrollbarGrab);
        COLOR_NAMES.put("scrollbarGrabHovered", ImGuiCol.ScrollbarGrabHovered);
        COLOR_NAMES.put("scrollbarGrabActive", ImGuiCol.ScrollbarGrabActive);
        COLOR_NAMES.put("checkMark", ImGuiCol.CheckMark);
        COLOR_NAMES.put("sliderGrab", ImGuiCol.SliderGrab);
        COLOR_NAMES.put("sliderGrabActive", ImGuiCol.SliderGrabActive);
        COLOR_NAMES.put("button", ImGuiCol.Button);
        COLOR_NAMES.put("buttonHovered", ImGuiCol.ButtonHovered);
        COLOR_NAMES.put("buttonActive", ImGuiCol.ButtonActive);
        COLOR_NAMES.put("header", ImGuiCol.Header);
        COLOR_NAMES.put("headerHovered", ImGuiCol.HeaderHovered);
        COLOR_NAMES.put("headerActive", ImGuiCol.HeaderActive);
        COLOR_NAMES.put("separator", ImGuiCol.Separator);
        COLOR_NAMES.put("separatorHovered", ImGuiCol.SeparatorHovered);
        COLOR_NAMES.put("separatorActive", ImGuiCol.SeparatorActive);
        COLOR_NAMES.put("resizeGrip", ImGuiCol.ResizeGrip);
        COLOR_NAMES.put("resizeGripHovered", ImGuiCol.ResizeGripHovered);
        COLOR_NAMES.put("resizeGripActive", ImGuiCol.ResizeGripActive);
        COLOR_NAMES.put("tab", ImGuiCol.Tab);
        COLOR_NAMES.put("tabHovered", ImGuiCol.TabHovered);
        COLOR_NAMES.put("tabActive", ImGuiCol.TabActive);
        COLOR_NAMES.put("tabUnfocused", ImGuiCol.TabUnfocused);
        COLOR_NAMES.put("tabUnfocusedActive", ImGuiCol.TabUnfocusedActive);
        COLOR_NAMES.put("dockingPreview", ImGuiCol.DockingPreview);
        COLOR_NAMES.put("dockingEmptyBg", ImGuiCol.DockingEmptyBg);
        COLOR_NAMES.put("plotLines", ImGuiCol.PlotLines);
        COLOR_NAMES.put("plotLinesHovered", ImGuiCol.PlotLinesHovered);
        COLOR_NAMES.put("plotHistogram", ImGuiCol.PlotHistogram);
        COLOR_NAMES.put("plotHistogramHovered", ImGuiCol.PlotHistogramHovered);
        COLOR_NAMES.put("tableHeaderBg", ImGuiCol.TableHeaderBg);
        COLOR_NAMES.put("tableBorderStrong", ImGuiCol.TableBorderStrong);
        COLOR_NAMES.put("tableBorderLight", ImGuiCol.TableBorderLight);
        COLOR_NAMES.put("tableRowBg", ImGuiCol.TableRowBg);
        COLOR_NAMES.put("tableRowBgAlt", ImGuiCol.TableRowBgAlt);
        COLOR_NAMES.put("textSelectedBg", ImGuiCol.TextSelectedBg);
        COLOR_NAMES.put("dragDropTarget", ImGuiCol.DragDropTarget);
        COLOR_NAMES.put("navHighlight", ImGuiCol.NavHighlight);
        COLOR_NAMES.put("navWindowingHighlight", ImGuiCol.NavWindowingHighlight);
        COLOR_NAMES.put("navWindowingDimBg", ImGuiCol.NavWindowingDimBg);
        COLOR_NAMES.put("modalWindowDimBg", ImGuiCol.ModalWindowDimBg);
    }
}
package com.igrium.craftui.style;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
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


    public static final BiMap<String, Integer> COLOR_NAMES = ImmutableBiMap.<String, Integer>builder()
            .put("text", ImGuiCol.Text)
            .put("textDisabled", ImGuiCol.TextDisabled)
            .put("windowBg", ImGuiCol.WindowBg)
            .put("childBg", ImGuiCol.ChildBg)
            .put("popupBg", ImGuiCol.PopupBg)
            .put("border", ImGuiCol.Border)
            .put("borderShadow", ImGuiCol.BorderShadow)
            .put("frameBg", ImGuiCol.FrameBg)
            .put("frameBgHovered", ImGuiCol.FrameBgHovered)
            .put("frameBgActive", ImGuiCol.FrameBgActive)
            .put("titleBg", ImGuiCol.TitleBg)
            .put("titleBgActive", ImGuiCol.TitleBgActive)
            .put("titleBgCollapsed", ImGuiCol.TitleBgCollapsed)
            .put("menuBarBg", ImGuiCol.MenuBarBg)
            .put("scrollbarBg", ImGuiCol.ScrollbarBg)
            .put("scrollbarGrab", ImGuiCol.ScrollbarGrab)
            .put("scrollbarGrabHovered", ImGuiCol.ScrollbarGrabHovered)
            .put("scrollbarGrabActive", ImGuiCol.ScrollbarGrabActive)
            .put("checkMark", ImGuiCol.CheckMark)
            .put("sliderGrab", ImGuiCol.SliderGrab)
            .put("sliderGrabActive", ImGuiCol.SliderGrabActive)
            .put("button", ImGuiCol.Button)
            .put("buttonHovered", ImGuiCol.ButtonHovered)
            .put("buttonActive", ImGuiCol.ButtonActive)
            .put("header", ImGuiCol.Header)
            .put("headerHovered", ImGuiCol.HeaderHovered)
            .put("headerActive", ImGuiCol.HeaderActive)
            .put("separator", ImGuiCol.Separator)
            .put("separatorHovered", ImGuiCol.SeparatorHovered)
            .put("separatorActive", ImGuiCol.SeparatorActive)
            .put("resizeGrip", ImGuiCol.ResizeGrip)
            .put("resizeGripHovered", ImGuiCol.ResizeGripHovered)
            .put("resizeGripActive", ImGuiCol.ResizeGripActive)
            .put("tab", ImGuiCol.Tab)
            .put("tabHovered", ImGuiCol.TabHovered)
            .put("tabActive", ImGuiCol.TabActive)
            .put("tabUnfocused", ImGuiCol.TabUnfocused)
            .put("tabUnfocusedActive", ImGuiCol.TabUnfocusedActive)
            .put("dockingPreview", ImGuiCol.DockingPreview)
            .put("dockingEmptyBg", ImGuiCol.DockingEmptyBg)
            .put("plotLines", ImGuiCol.PlotLines)
            .put("plotLinesHovered", ImGuiCol.PlotLinesHovered)
            .put("plotHistogram", ImGuiCol.PlotHistogram)
            .put("plotHistogramHovered", ImGuiCol.PlotHistogramHovered)
            .put("tableHeaderBg", ImGuiCol.TableHeaderBg)
            .put("tableBorderStrong", ImGuiCol.TableBorderStrong)
            .put("tableBorderLight", ImGuiCol.TableBorderLight)
            .put("tableRowBg", ImGuiCol.TableRowBg)
            .put("tableRowBgAlt", ImGuiCol.TableRowBgAlt)
            .put("textSelectedBg", ImGuiCol.TextSelectedBg)
            .put("dragDropTarget", ImGuiCol.DragDropTarget)
            .put("navHighlight", ImGuiCol.NavHighlight)
            .put("navWindowingHighlight", ImGuiCol.NavWindowingHighlight)
            .put("navWindowingDimBg", ImGuiCol.NavWindowingDimBg)
            .put("modalWindowDimBg", ImGuiCol.ModalWindowDimBg)
            .build();
}
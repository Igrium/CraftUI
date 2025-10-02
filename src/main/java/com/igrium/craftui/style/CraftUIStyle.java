package com.igrium.craftui.style;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.igrium.craftui.impl.util.IdentifierJsonAdapter;
import imgui.ImGuiStyle;
import imgui.ImVec2;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an ImGUI style in Java, allowing for json serialization
 */
public class CraftUIStyle {

    /**
     * A dead-simple vector implementation for easy serialization.
     */
    @JsonAdapter(StyleVec2JsonAdapter.class)
    public record StyleVec2(float x, float y) {
        public StyleVec2() {
            this(0, 0);
        }

        public StyleVec2(ImVec2 vec) {
            this(vec.x, vec.y);
        }
    }

    private static class StyleVec2JsonAdapter extends TypeAdapter<StyleVec2> {

        @Override
        public void write(JsonWriter out, StyleVec2 value) throws IOException {
            out.beginArray();
            out.value(value.x());
            out.value(value.y());
            out.endArray();
        }

        @Override
        public StyleVec2 read(JsonReader in) throws IOException {
            in.beginArray();
            float x = (float) in.nextDouble();
            float y = (float) in.nextDouble();
            in.endArray();
            return new StyleVec2(x, y);
        }
    }

    @JsonAdapter(IdentifierJsonAdapter.class)
    @Getter @Setter @Nullable
    Identifier defaultFont;

    @Getter @Setter
    float alpha;

    @Getter @Setter
    float disabledAlpha;

    @Getter @Setter @NonNull
    CraftUIStyle.StyleVec2 windowPadding = new StyleVec2();

    @Getter @Setter
    float windowRounding;

    @Getter @Setter
    float windowBorderSize;

    @Getter @Setter @NonNull
    CraftUIStyle.StyleVec2 windowMinSize = new StyleVec2();

    @Getter @Setter @NonNull
    CraftUIStyle.StyleVec2 windowTitleAlign = new StyleVec2();

    @Getter @Setter
    int windowMenuButtonPosition;

    @Getter @Setter
    float childRounding;

    @Getter @Setter
    float childBorderSize;

    @Getter @Setter
    float popupRounding;

    @Getter @Setter
    float popupBorderSize;

    @Getter @Setter @NonNull
    CraftUIStyle.StyleVec2 framePadding = new StyleVec2();

    @Getter @Setter
    float frameRounding;

    @Getter @Setter
    float frameBorderSize;

    @Getter @Setter @NonNull
    CraftUIStyle.StyleVec2 itemSpacing = new StyleVec2();

    @Getter @Setter @NonNull
    CraftUIStyle.StyleVec2 itemInnerSpacing = new StyleVec2();

    @Getter @Setter @NonNull
    CraftUIStyle.StyleVec2 cellPadding = new StyleVec2();

    @Getter @Setter @NonNull
    CraftUIStyle.StyleVec2 touchExtraPadding = new StyleVec2();

    @Getter @Setter
    float indentSpacing;

    @Getter @Setter
    float columnsMinSpacing;

    @Getter @Setter
    float scrollbarSize;

    @Getter @Setter
    float scrollbarRounding;

    @Getter @Setter
    float grabMinSize;

    @Getter @Setter
    float grabRounding;

    @Getter @Setter
    float logSliderDeadzone;

    @Getter @Setter
    float tabRounding;
    
    @Getter @Setter
    float tabBorderSize;
    
    @Getter @Setter
    float tabMinWidthForCloseButton;
    
    @Getter @Setter
    int colorButtonPosition;
    
    @Getter @Setter @NonNull
    CraftUIStyle.StyleVec2 buttonTextAlign = new StyleVec2();

    @Getter @Setter @NonNull
    CraftUIStyle.StyleVec2 selectableTextAlign = new StyleVec2();

    @Getter @Setter @NonNull
    CraftUIStyle.StyleVec2 displayWindowPadding = new StyleVec2();

    @Getter @Setter @NonNull
    CraftUIStyle.StyleVec2 displaySafeAreaPadding = new StyleVec2();

    @Getter @Setter
    float mouseCursorScale;

    @Getter @Setter
    boolean antiAliasedLines;

    @Getter @Setter
    boolean antiAliasedLinesUseTex;

    @Getter @Setter
    boolean antiAliasedFill;

    @Getter @Setter
    float curveTessellationTol;

    @Getter @Setter
    float circleTessellationMaxError;

    @Getter @NonNull
    final Map<String, CraftUIColors.UIColor> colors = new HashMap<>();

    /**
     * Convert this style into a native ImGui style.
     * @return ImGui style.
     */
    public ImGuiStyle buildStyle() {
        ImGuiStyle style = new ImGuiStyle();
        buildStyle(style);
        return style;
    }

    /**
     * Convert this style into a native ImGui style.
     * @param style Style to append to.
     */
    public void buildStyle(ImGuiStyle style) {
        style.setAlpha(alpha);
        style.setDisabledAlpha(disabledAlpha);
        style.setWindowPadding(windowPadding.x, windowPadding.y);
        style.setWindowRounding(windowRounding);
        style.setWindowBorderSize(windowBorderSize);
        style.setWindowMinSize(windowMinSize.x, windowMinSize.y);
        style.setWindowTitleAlign(windowTitleAlign.x, windowTitleAlign.y);
        style.setWindowMenuButtonPosition(windowMenuButtonPosition);
        style.setChildRounding(childRounding);
        style.setChildBorderSize(childBorderSize);
        style.setPopupRounding(popupRounding);
        style.setPopupBorderSize(popupBorderSize);
        style.setFramePadding(framePadding.x, framePadding.y);
        style.setFrameRounding(frameRounding);
        style.setFrameBorderSize(frameBorderSize);
        style.setItemSpacing(itemSpacing.x, itemSpacing.y);
        style.setItemInnerSpacing(itemInnerSpacing.x, itemInnerSpacing.y);
        style.setCellPadding(cellPadding.x, cellPadding.y);
        style.setTouchExtraPadding(touchExtraPadding.x, touchExtraPadding.y);
        style.setIndentSpacing(indentSpacing);
        style.setColumnsMinSpacing(columnsMinSpacing);
        style.setScrollbarSize(scrollbarSize);
        style.setScrollbarRounding(scrollbarRounding);
        style.setGrabMinSize(grabMinSize);
        style.setGrabRounding(grabRounding);
        style.setLogSliderDeadzone(logSliderDeadzone);
        style.setTabRounding(tabRounding);
        style.setTabBorderSize(tabBorderSize);
        style.setTabMinWidthForCloseButton(tabMinWidthForCloseButton);
        style.setColorButtonPosition(colorButtonPosition);
        style.setButtonTextAlign(buttonTextAlign.x, buttonTextAlign.y);
        style.setSelectableTextAlign(selectableTextAlign.x, selectableTextAlign.y);
        style.setDisplayWindowPadding(displayWindowPadding.x, displayWindowPadding.y);
        style.setDisplaySafeAreaPadding(displaySafeAreaPadding.x, displaySafeAreaPadding.y);
        style.setMouseCursorScale(mouseCursorScale);
        style.setAntiAliasedLines(antiAliasedLines);
        style.setAntiAliasedLinesUseTex(antiAliasedLinesUseTex);
        style.setAntiAliasedFill(antiAliasedFill);
        style.setCurveTessellationTol(curveTessellationTol);
        style.setCircleTessellationMaxError(circleTessellationMaxError);

        CraftUIColors.buildStyle(style, colors);
    }

    public void fromNativeStyle(ImGuiStyle style) {
        this.alpha = style.getAlpha();
        this.disabledAlpha = style.getDisabledAlpha();
        this.windowPadding = new StyleVec2(style.getWindowPadding());
        this.windowRounding = style.getWindowRounding();
        this.windowBorderSize = style.getWindowBorderSize();
        this.windowMinSize = new StyleVec2(style.getWindowMinSize());
        this.windowTitleAlign = new StyleVec2(style.getWindowTitleAlign());
        this.windowMenuButtonPosition = style.getWindowMenuButtonPosition();
        this.childRounding = style.getChildRounding();
        this.childBorderSize = style.getChildBorderSize();
        this.popupRounding = style.getPopupRounding();
        this.popupBorderSize = style.getPopupBorderSize();
        this.framePadding = new StyleVec2(style.getFramePadding());
        this.frameRounding = style.getFrameRounding();
        this.frameBorderSize = style.getFrameBorderSize();
        this.itemSpacing = new StyleVec2(style.getItemSpacing());
        this.itemInnerSpacing = new StyleVec2(style.getItemInnerSpacing());
        this.cellPadding = new StyleVec2(style.getCellPadding());
        this.touchExtraPadding = new StyleVec2(style.getTouchExtraPadding());
        this.indentSpacing = style.getIndentSpacing();
        this.columnsMinSpacing = style.getColumnsMinSpacing();
        this.scrollbarSize = style.getScrollbarSize();
        this.scrollbarRounding = style.getScrollbarRounding();
        this.grabMinSize = style.getGrabMinSize();
        this.grabRounding = style.getGrabRounding();
        this.logSliderDeadzone = style.getLogSliderDeadzone();
        this.tabRounding = style.getTabRounding();
        this.tabBorderSize = style.getTabBorderSize();
        this.tabMinWidthForCloseButton = style.getTabMinWidthForCloseButton();
        this.colorButtonPosition = style.getColorButtonPosition();
        this.buttonTextAlign = new StyleVec2(style.getButtonTextAlign());
        this.selectableTextAlign = new StyleVec2(style.getSelectableTextAlign());
        this.displayWindowPadding = new StyleVec2(style.getDisplayWindowPadding());
        this.displaySafeAreaPadding = new StyleVec2(style.getDisplaySafeAreaPadding());
        this.mouseCursorScale = style.getMouseCursorScale();
        this.antiAliasedLines = style.getAntiAliasedLines();
        this.antiAliasedLinesUseTex = style.getAntiAliasedLinesUseTex();
        this.antiAliasedFill = style.getAntiAliasedFill();
        this.curveTessellationTol = style.getCurveTessellationTol();
        this.circleTessellationMaxError = style.getCircleTessellationMaxError();

        colors.clear();
        colors.putAll(CraftUIColors.fromStyle(style));
    }
}

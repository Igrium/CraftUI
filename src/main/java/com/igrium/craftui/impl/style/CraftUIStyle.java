package com.igrium.craftui.impl.style;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.igrium.craftui.impl.util.IdentifierJsonAdapter;
import imgui.ImGuiStyle;
import imgui.ImVec2;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class CraftUIStyle {

    @JsonAdapter(SimpleVec2JsonAdapter.class)
    public record SimpleVec2(float x, float y) {
        public SimpleVec2() {
            this(0, 0);
        }

        ImVec2 toIm() {
            return new ImVec2(x, y);
        }
    }

    private static class SimpleVec2JsonAdapter extends TypeAdapter<SimpleVec2> {

        @Override
        public void write(JsonWriter out, SimpleVec2 value) throws IOException {
            out.beginArray();
            out.value(value.x());
            out.value(value.y());
            out.endArray();
        }

        @Override
        public SimpleVec2 read(JsonReader in) throws IOException {
            in.beginArray();
            float x = (float) in.nextDouble();
            float y = (float) in.nextDouble();
            in.endArray();
            return new SimpleVec2(x, y);
        }
    }

    @JsonAdapter(IdentifierJsonAdapter.class)
    @Getter @Setter @Nullable
    Identifier defaultFont;

    @Getter @Setter @Nullable
    Float alpha;

    @Getter @Setter @Nullable
    Float disabledAlpha;

    @Getter @Setter @Nullable
    SimpleVec2 windowPadding;

    @Getter @Setter @Nullable
    Float windowRounding;

    @Getter @Setter @Nullable
    Float windowBorderSize;

    @Getter @Setter @Nullable
    SimpleVec2 windowMinSize;

    @Getter @Setter @Nullable
    SimpleVec2 windowTitleAlign;

    @Getter @Setter @Nullable
    Integer windowMenuButtonPosition;

    @Getter @Setter @Nullable
    Float childRounding;

    @Getter @Setter @Nullable
    Float childBorderSize;

    @Getter @Setter @Nullable
    Float popupRounding;

    @Getter @Setter @Nullable
    Float popupBorderSize;

    @Getter @Setter @Nullable
    SimpleVec2 framePadding;

    @Getter @Setter @Nullable
    Float frameRounding;

    @Getter @Setter @Nullable
    Float frameBorderSize;

    @Getter @Setter @Nullable
    SimpleVec2 itemSpacing;

    @Getter @Setter @Nullable
    SimpleVec2 itemInnerSpacing;

    @Getter @Setter @Nullable
    SimpleVec2 cellPadding;

    @Getter @Setter @Nullable
    SimpleVec2 touchExtraPadding;

    @Getter @Setter @Nullable
    Float indentSpacing;

    @Getter @Setter @Nullable
    Float columnsMinSpacing;

    @Getter @Setter @Nullable
    Float scrollbarSize;

    @Getter @Setter @Nullable
    Float scrollbarRounding;

    @Getter @Setter @Nullable
    Float grabMinSize;

    @Getter @Setter @Nullable
    Float grabRounding;

    @Getter @Setter @Nullable
    Float logSliderDeadzone;

    @Getter @Setter @Nullable
    Float tabRounding;
    
    @Getter @Setter @Nullable
    Float tabBorderSize;
    
    @Getter @Setter @Nullable
    Float tabMinWidthForCloseButton;
    
    @Getter @Setter @Nullable
    Integer colorButtonPosition;
    
    @Getter @Setter @Nullable
    SimpleVec2 buttonTextAlign;

    @Getter @Setter @Nullable
    SimpleVec2 selectableTextAlign;

    @Getter @Setter @Nullable
    SimpleVec2 displayWindowPadding;

    @Getter @Setter @Nullable
    SimpleVec2 displaySafeAreaPadding;

    @Getter @Setter @Nullable
    Float mouseCursorScale;

    @Getter @Setter @Nullable
    Boolean antiAliasedLines;

    @Getter @Setter @Nullable
    Boolean antiAliasedLinesUseTex;

    @Getter @Setter @Nullable
    Boolean antiAliasedFill;

    @Getter @Setter @Nullable
    Float curveTessellationTol;

    @Getter @Setter @Nullable
    Float circleTessellationMaxError;

    @Getter @NotNull
    final StyleColorMap colors = new StyleColorMap();

    public void buildStyle(ImGuiStyle style) {
        if (alpha != null) style.setAlpha(alpha);
        if (disabledAlpha != null) style.setDisabledAlpha(disabledAlpha);
        if (windowPadding != null) style.setWindowPadding(windowPadding.x, windowPadding.y);
        if (windowRounding != null) style.setWindowRounding(windowRounding);
        if (windowBorderSize != null) style.setWindowBorderSize(windowBorderSize);
        if (windowMinSize != null) style.setWindowMinSize(windowMinSize.x, windowMinSize.y);
        if (windowTitleAlign != null) style.setWindowTitleAlign(windowTitleAlign.x, windowTitleAlign.y);
        if (windowMenuButtonPosition != null) style.setWindowMenuButtonPosition(windowMenuButtonPosition);
        if (childRounding != null) style.setChildRounding(childRounding);
        if (childBorderSize != null) style.setChildBorderSize(childBorderSize);
        if (popupRounding != null) style.setPopupRounding(popupRounding);
        if (popupBorderSize != null) style.setPopupBorderSize(popupBorderSize);
        if (framePadding != null) style.setFramePadding(framePadding.x, framePadding.y);
        if (frameRounding != null) style.setFrameRounding(frameRounding);
        if (frameBorderSize != null) style.setFrameBorderSize(frameBorderSize);
        if (itemSpacing != null) style.setItemSpacing(itemSpacing.x, itemSpacing.y);
        if (itemInnerSpacing != null) style.setItemInnerSpacing(itemInnerSpacing.x, itemInnerSpacing.y);
        if (cellPadding != null) style.setCellPadding(cellPadding.x, cellPadding.y);
        if (touchExtraPadding != null) style.setTouchExtraPadding(touchExtraPadding.x, touchExtraPadding.y);
        if (indentSpacing != null) style.setIndentSpacing(indentSpacing);
        if (columnsMinSpacing != null) style.setColumnsMinSpacing(columnsMinSpacing);
        if (scrollbarSize != null) style.setScrollbarSize(scrollbarSize);
        if (scrollbarRounding != null) style.setScrollbarRounding(scrollbarRounding);
        if (grabMinSize != null) style.setGrabMinSize(grabMinSize);
        if (grabRounding != null) style.setGrabRounding(grabRounding);
        if (logSliderDeadzone != null) style.setLogSliderDeadzone(logSliderDeadzone);
        if (tabRounding != null) style.setTabRounding(tabRounding);
        if (tabBorderSize != null) style.setTabBorderSize(tabBorderSize);
        if (tabMinWidthForCloseButton != null) style.setTabMinWidthForCloseButton(tabMinWidthForCloseButton);
        if (colorButtonPosition != null) style.setColorButtonPosition(colorButtonPosition);
        if (buttonTextAlign != null) style.setButtonTextAlign(buttonTextAlign.x, buttonTextAlign.y);
        if (selectableTextAlign != null) style.setSelectableTextAlign(selectableTextAlign.x, selectableTextAlign.y);
        if (displayWindowPadding != null) style.setDisplayWindowPadding(displayWindowPadding.x, displayWindowPadding.y);
        if (displaySafeAreaPadding != null) style.setDisplaySafeAreaPadding(displaySafeAreaPadding.x, displaySafeAreaPadding.y);
        if (mouseCursorScale != null) style.setMouseCursorScale(mouseCursorScale);
        if (antiAliasedLines != null) style.setAntiAliasedLines(antiAliasedLines);
        if (antiAliasedLinesUseTex != null) style.setAntiAliasedLinesUseTex(antiAliasedLinesUseTex);
        if (antiAliasedFill != null) style.setAntiAliasedFill(antiAliasedFill);
        if (curveTessellationTol != null) style.setCurveTessellationTol(curveTessellationTol);
        if (circleTessellationMaxError != null) style.setCircleTessellationMaxError(circleTessellationMaxError);

        colors.buildStyle(style);
    }


    public void appendAll(CraftUIStyle other) {
        if (other == null) return;

        if (other.defaultFont != null) this.defaultFont = other.defaultFont;
        if (other.alpha != null) this.alpha = other.alpha;
        if (other.disabledAlpha != null) this.disabledAlpha = other.disabledAlpha;
        if (other.windowPadding != null) this.windowPadding = other.windowPadding;
        if (other.windowRounding != null) this.windowRounding = other.windowRounding;
        if (other.windowBorderSize != null) this.windowBorderSize = other.windowBorderSize;
        if (other.windowMinSize != null) this.windowMinSize = other.windowMinSize;
        if (other.windowTitleAlign != null) this.windowTitleAlign = other.windowTitleAlign;
        if (other.windowMenuButtonPosition != null) this.windowMenuButtonPosition = other.windowMenuButtonPosition;
        if (other.childRounding != null) this.childRounding = other.childRounding;
        if (other.childBorderSize != null) this.childBorderSize = other.childBorderSize;
        if (other.popupRounding != null) this.popupRounding = other.popupRounding;
        if (other.popupBorderSize != null) this.popupBorderSize = other.popupBorderSize;
        if (other.framePadding != null) this.framePadding = other.framePadding;
        if (other.frameRounding != null) this.frameRounding = other.frameRounding;
        if (other.frameBorderSize != null) this.frameBorderSize = other.frameBorderSize;
        if (other.itemSpacing != null) this.itemSpacing = other.itemSpacing;
        if (other.itemInnerSpacing != null) this.itemInnerSpacing = other.itemInnerSpacing;
        if (other.cellPadding != null) this.cellPadding = other.cellPadding;
        if (other.touchExtraPadding != null) this.touchExtraPadding = other.touchExtraPadding;
        if (other.indentSpacing != null) this.indentSpacing = other.indentSpacing;
        if (other.columnsMinSpacing != null) this.columnsMinSpacing = other.columnsMinSpacing;
        if (other.scrollbarSize != null) this.scrollbarSize = other.scrollbarSize;
        if (other.scrollbarRounding != null) this.scrollbarRounding = other.scrollbarRounding;
        if (other.grabMinSize != null) this.grabMinSize = other.grabMinSize;
        if (other.grabRounding != null) this.grabRounding = other.grabRounding;
        if (other.logSliderDeadzone != null) this.logSliderDeadzone = other.logSliderDeadzone;
        if (other.tabRounding != null) this.tabRounding = other.tabRounding;
        if (other.tabBorderSize != null) this.tabBorderSize = other.tabBorderSize;
        if (other.tabMinWidthForCloseButton != null) this.tabMinWidthForCloseButton = other.tabMinWidthForCloseButton;
        if (other.colorButtonPosition != null) this.colorButtonPosition = other.colorButtonPosition;
        if (other.buttonTextAlign != null) this.buttonTextAlign = other.buttonTextAlign;
        if (other.selectableTextAlign != null) this.selectableTextAlign = other.selectableTextAlign;
        if (other.displayWindowPadding != null) this.displayWindowPadding = other.displayWindowPadding;
        if (other.displaySafeAreaPadding != null) this.displaySafeAreaPadding = other.displaySafeAreaPadding;
        if (other.mouseCursorScale != null) this.mouseCursorScale = other.mouseCursorScale;
        if (other.antiAliasedLines != null) this.antiAliasedLines = other.antiAliasedLines;
        if (other.antiAliasedLinesUseTex != null) this.antiAliasedLinesUseTex = other.antiAliasedLinesUseTex;
        if (other.antiAliasedFill != null) this.antiAliasedFill = other.antiAliasedFill;
        if (other.curveTessellationTol != null) this.curveTessellationTol = other.curveTessellationTol;
        if (other.circleTessellationMaxError != null) this.circleTessellationMaxError = other.circleTessellationMaxError;

        this.colors.appendAll(other.colors);
    }
}

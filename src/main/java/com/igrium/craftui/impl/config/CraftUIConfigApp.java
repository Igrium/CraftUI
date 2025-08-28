package com.igrium.craftui.impl.config;

import com.igrium.craftui.app.AppManager;
import com.igrium.craftui.config.CraftUIConfig;
import com.igrium.craftui.screen.CraftAppScreen;
import com.igrium.craftui.CraftUI;
import com.igrium.craftui.app.CraftApp;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class CraftUIConfigApp extends CraftApp {

//    private final SaveConfirmation saveConfirmation = new SaveConfirmation(this::save, this::close);

    private final CraftUIConfig config = CraftUI.getConfig();

    private final ImBoolean preferNativeFileDialog = new ImBoolean(config.isPreferNativeFileDialog());
    private final ImBoolean enableViewports = new ImBoolean(config.isEnableViewports());
    private final ImBoolean layoutPersistent = new ImBoolean(config.isLayoutPersistent());
    private final ImBoolean enableDebugCommand = new ImBoolean(config.isEnableDebugCommands());

    private boolean isUnsaved;

    @Override
    protected void render(MinecraftClient client) {
        
        var viewport = ImGui.getMainViewport();

        ImGui.setNextWindowPos(viewport.getCenterX(), viewport.getCenterY(), ImGuiCond.Always, .5f, .5f);
        if (ImGui.begin(
                Text.translatable("options.craftui.header").getString(), ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove)) {

            checkbox("options.craftui.preferNativeFileDialog", preferNativeFileDialog, "options.craftui.preferNativeFileDialog.tooltip");
            checkbox("options.craftui.enableViewports", enableViewports, "options.craftui.enableViewports.tooltip");
            checkbox("layoutPersistent", layoutPersistent, "layoutPersistent.tooltip");

            if (button(Text.translatable("options.craftui.resetLayout"), Text.translatable("options.craftui.resetLayout.tooltip"))) {
                AppManager.resetUiLayouts();
            }

            checkbox("options.craftui.enableDebugCommands", enableDebugCommand, "options.craftui.enableDebugCommands.tooltip");

            ImGui.separator();

            if (ImGui.button(isUnsaved ? "Apply" : "Close")) {
                if (isUnsaved) {
                    save();
                }
                close();
            };

        }
        ImGui.end();
    }

    private boolean checkbox(String translationKey, ImBoolean active, @Nullable String helpTranslationKey) {
        return checkbox(Text.translatable(translationKey), active, Text.translatable(helpTranslationKey));
    }

    private boolean checkbox(Text name, ImBoolean active, @Nullable Text helpText) {
        boolean updated = ImGui.checkbox(name.getString(), active);
        if (helpText != null && ImGui.isItemHovered(ImGuiHoveredFlags.AllowWhenDisabled)) {
            ImGui.setTooltip(helpText.getString());
        }
        if (updated) {
            onUpdate();
        }
        return updated;
    }

    private boolean button(Text name, @Nullable Text toolTip) {
        boolean pressed = ImGui.button(name.getString());
        if (toolTip != null && ImGui.isItemHovered(ImGuiHoveredFlags.AllowWhenDisabled)) {
            ImGui.setTooltip(toolTip.getString());
        }
        return pressed;
    }

    private void onUpdate() {
        isUnsaved = true;
    }
    
    private void save() {
        config.setPreferNativeFileDialog(preferNativeFileDialog.get());
        config.setEnableViewports(enableViewports.get());
        config.setLayoutPersistent(layoutPersistent.get());
        config.setEnableDebugCommands(enableDebugCommand.get());
        CraftUI.saveConfig();

        isUnsaved = false;
    }

    public static CraftAppScreen<CraftUIConfigApp> createScreen() {
        return new CraftAppScreen<>(new CraftUIConfigApp());
    }
}

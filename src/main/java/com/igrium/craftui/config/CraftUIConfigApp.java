package com.igrium.craftui.config;

import com.igrium.craftui.app.AppManager;
import com.igrium.craftui.screen.CraftAppScreen;
import com.igrium.craftui.CraftUI;
import com.igrium.craftui.app.CraftApp;
import com.igrium.craftui.util.SaveConfirmation;
import com.igrium.craftui.util.TextUtils;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiHoveredFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

public class CraftUIConfigApp extends CraftApp {

    private final SaveConfirmation saveConfirmation = new SaveConfirmation(this::save, this::close);

    private final CraftUIConfig config = CraftUI.getConfig();

    private final ImBoolean preferNativeFileDialog = new ImBoolean(config.isPreferNativeFileDialog());
    private final ImBoolean enableViewports = new ImBoolean(config.isEnableViewports());
    private final ImBoolean layoutPersistent = new ImBoolean(config.isLayoutPersistent());
    private final ImBoolean enableDebugCommand = new ImBoolean(config.isEnableDebugCommands());

    @Override
    protected void render(MinecraftClient client) {
        
        var viewport = ImGui.getMainViewport();

        ImGui.setNextWindowPos(viewport.getCenterX(), viewport.getCenterY(), ImGuiCond.Always, .5f, .5f);
        if (ImGui.begin(
                TextUtils.getString(Text.translatable("options.craftui.header")), ImGuiWindowFlags.NoCollapse
                | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoMove)) {

            checkbox("options.craftui.preferNativeFileDialog", preferNativeFileDialog, "options.craftui.preferNativeFileDialog.tooltip");
            checkbox("options.craftui.enableViewports", enableViewports, "options.craftui.enableViewports.tooltip");
            checkbox("layoutPersistent", layoutPersistent, "layoutPersistent.tooltip");

            if (button(Text.translatable("options.craftui.resetLayout"), Text.translatable("options.craftui.resetLayout.tooltip"))) {
                AppManager.resetUiLayouts();
            }

            checkbox("options.craftui.enableDebugCommands", enableDebugCommand, "options.craftui.enableDebugCommands.tooltip");

            ImGui.separator();

            if (ImGui.button(saveConfirmation.isUnsaved() ? "Apply" : "Close")) {
                save();
                saveConfirmation.tryClose();
            };

            if (ImGui.isKeyPressed(ImGuiKey.Space)) {
                saveConfirmation.tryClose();
            }
            saveConfirmation.render();
        }
        ImGui.end();
    }

    private boolean checkbox(String translationKey, ImBoolean active, @Nullable String helpTranslationKey) {
        return checkbox(Text.translatable(translationKey), active, Text.translatable(helpTranslationKey));
    }

    private boolean checkbox(Text name, ImBoolean active, @Nullable Text helpText) {
        boolean updated = ImGui.checkbox(TextUtils.getString(name), active);
        if (helpText != null && ImGui.isItemHovered(ImGuiHoveredFlags.AllowWhenDisabled)) {
            ImGui.setTooltip(TextUtils.getString(helpText));
        }
        if (updated) {
            onUpdate();
        }
        return updated;
    }

    private boolean button(Text name, @Nullable Text toolTip) {
        boolean pressed = ImGui.button(TextUtils.getString(name));
        if (toolTip != null && ImGui.isItemHovered(ImGuiHoveredFlags.AllowWhenDisabled)) {
            ImGui.setTooltip(TextUtils.getString(toolTip));
        }
        return pressed;
    }

    private void onUpdate() {
        saveConfirmation.setUnsaved(true);
    }
    
    private void save() {
        config.setPreferNativeFileDialog(preferNativeFileDialog.get());
        config.setEnableViewports(enableViewports.get());
        config.setLayoutPersistent(layoutPersistent.get());
        config.setEnableDebugCommands(enableDebugCommand.get());
        CraftUIConfigHandler.saveConfig();

        saveConfirmation.setUnsaved(false);
    }

    public static CraftAppScreen<CraftUIConfigApp> createScreen() {
        return new CraftAppScreen<>(new CraftUIConfigApp());
    }
}

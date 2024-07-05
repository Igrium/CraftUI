package com.igrium.craftui.app;

import com.igrium.craftui.CraftAppScreen;
import com.igrium.craftui.CraftUI;
import com.igrium.craftui.util.SaveConfirmation;
import com.igrium.craftui.util.TextUtils;

import imgui.ImGui;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class CraftUIOptionsApp extends CraftApp {

    private SaveConfirmation saveConfirmation = new SaveConfirmation(this::save, this::close);

    private ImBoolean preferNativeFileDialog = new ImBoolean(CraftUI.getConfig().preferNativeFileDialog());
    private ImBoolean enableViewports = new ImBoolean(CraftUI.getConfig().enableViewports());

    @Override
    protected void render(MinecraftClient client) {
        
        ImGui.getMainViewport();

        if (ImGui.begin(
                "CraftUI Options", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDecoration
                | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoMove)) {
            checkbox("options.craftui.preferNativeFileDialog", preferNativeFileDialog);
            checkbox("options.craftui.enableViewports", enableViewports);

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

    private boolean checkbox(String translationKey, ImBoolean active) {
        if (ImGui.checkbox(TextUtils.getString(Text.translatable(translationKey)), active)) {
            onUpdate();
            return true;
        };
        return false;
    }
    

    private void onUpdate() {
        saveConfirmation.setUnsaved(true);
    }
    
    private void save() {
        CraftUI.getConfig().setPreferNativeFileDialog(preferNativeFileDialog.get());
        CraftUI.getConfig().setEnableViewports(enableViewports.get());
        CraftUI.applyConfig(true);

        saveConfirmation.setUnsaved(false);
    }

    public static CraftAppScreen<CraftUIOptionsApp> createScreen() {
        CraftAppScreen<CraftUIOptionsApp> screen = new CraftAppScreen<>(new CraftUIOptionsApp());
        screen.setCloseOnEsc(false);
        return screen;
    }
}

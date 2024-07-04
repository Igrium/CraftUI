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

    @Override
    protected void render(MinecraftClient client) {
        
        ImGui.getMainViewport();

        if (ImGui.begin(
                "CraftUI Options", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDecoration
                | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoMove)) {
            if (checkbox("options.craftui.preferNativeFileDialog", preferNativeFileDialog)) {
                onUpdate();
            };

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

    private static final boolean checkbox(String translationKey, ImBoolean active) {
        return ImGui.checkbox(TextUtils.getString(Text.translatable(translationKey)), active);
    }

    private void onUpdate() {
        saveConfirmation.setUnsaved(true);
    }
    
    private void save() {
        CraftUI.getConfig().setPreferNativeFileDialog(preferNativeFileDialog.get());
        CraftUI.applyConfig(true);

        saveConfirmation.setUnsaved(false);
    }

    public static CraftAppScreen<CraftUIOptionsApp> createScreen() {
        CraftAppScreen<CraftUIOptionsApp> screen = new CraftAppScreen<>(new CraftUIOptionsApp());
        screen.setCloseOnEsc(false);
        return screen;
    }
}

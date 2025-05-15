package com.igrium.craftui.util;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import lombok.Getter;
import lombok.Setter;

/**
 * A widget that shows a confirmation screen when you try to close an app.
 */
public class SaveConfirmation {

    protected final Runnable saveRunnable;

    protected final Runnable closeRunnable;

    public SaveConfirmation(Runnable saveRunnable, Runnable closeRunnable) {
        this.saveRunnable = saveRunnable;
        this.closeRunnable = closeRunnable;
    }

    @Setter
    @Getter
    private boolean unsaved;

    @Getter
    @Setter
    private String title = "Unsaved Changes";

    @Getter
    @Setter
    private String text = "Do you want to save your changes?";

    @Getter
    private boolean confirmPopupShowing;

    /**
     * Call when the user wants to close the editor.
     */
    public void tryClose() {
        if (confirmPopupShowing) {
            return;
        }

        if (isUnsaved()) {
            ImGui.openPopup(title);
            confirmPopupShowing = true;
        } else {
            closeRunnable.run();
        }
    }

    public void render() {
        if (!confirmPopupShowing) {
            return;
        }

        ImVec2 center = ImGui.getMainViewport().getCenter();
        ImGui.setNextWindowPos(center.x, center.y, ImGuiCond.Appearing, .5f, .5f);

        if (ImGui.beginPopupModal(title, ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text(text);
            ImGui.separator();

            if (ImGui.button("Save", 120, 0)) {
                ImGui.closeCurrentPopup();
                confirmPopupShowing = false;
                saveRunnable.run();
                closeRunnable.run();
            }
            ImGui.setItemDefaultFocus();
            
            ImGui.sameLine();
            if (ImGui.button("Discard", 120, 0)) {
                ImGui.closeCurrentPopup();
                confirmPopupShowing = false;
                closeRunnable.run();
            }

            ImGui.sameLine();
            if (ImGui.button("Cancel", 120, 0)) {
                ImGui.closeCurrentPopup();
                confirmPopupShowing = false;
            }

            ImGui.endPopup();
        }
    }
}

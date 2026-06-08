package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.type.*;
import lombok.Setter;

class EditableText {
    private boolean isEditing;
    private boolean wasEditing;

    @Setter
    private boolean forceEdit;

    public boolean editString(String id, ImString text, float editWidth) {
        return doEdit(id, text.get(), editWidth,
                () -> ImGui.inputText("##" + id + ".input", text));
    }

    public boolean editFloat(String id, ImFloat value, float editWidth) {
        return doEdit(id, String.valueOf(value.get()), editWidth,
                () -> ImGui.inputFloat("##" + id + ".input", value));
    }

    public boolean editDouble(String id, ImDouble value, float editWidth) {
        return doEdit(id, String.valueOf(value.get()), editWidth,
                () -> ImGui.inputDouble("##" + id + ".input", value));
    }

    public boolean editInt(String id, ImInt value, float editWidth) {
        return doEdit(id, String.valueOf(value.get()), editWidth,
                () -> ImGui.inputScalar("##" + id + ".input", value));
    }

    public boolean editShort(String id, ImShort value, float editWidth) {
        return doEdit(id, String.valueOf(value.get()), editWidth,
                () -> ImGui.inputScalar("##" + id + ".input", value));
    }

    public boolean editLong(String id, ImLong value, float editWidth) {
        return doEdit(id, String.valueOf(value.get()), editWidth,
                () -> ImGui.inputScalar("##" + id + ".input", value));
    }

    private boolean doEdit(String id, String displayText, float editWidth, Runnable inputWidget) {
        boolean modified = false;
        if (isEditing) {
            if (!wasEditing) {
                ImGui.setKeyboardFocusHere();
            }
            ImGui.setNextItemWidth(Math.min(editWidth, ImGui.getContentRegionAvailX()));
            inputWidget.run();
            if (wasEditing && !ImGui.isItemActive()) {
                isEditing = false;
                wasEditing = false;
                modified = true;
            } else {
                wasEditing = true;
            }
        } else {
            selectableText(displayText + "##" + id + ".text");
            if (forceEdit || (ImGui.isItemHovered() && ImGui.getIO().getMouseDoubleClicked(0))) {
                isEditing = true;
            }
        }
        forceEdit = false;
        return modified;
    }

    private static void selectableText(String text) {
        ImGui.selectable(text, ImGui.calcTextSizeX(text, true), ImGui.calcTextSizeY(text, true));
    }
}
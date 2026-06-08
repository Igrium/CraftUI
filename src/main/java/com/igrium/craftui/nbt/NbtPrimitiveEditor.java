package com.igrium.craftui.nbt;

import com.igrium.craftui.icon.NbtIcons;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImString;
import net.minecraft.nbt.NbtElement;

public abstract sealed class NbtPrimitiveEditor<T extends NbtElement> extends NbtEditor<T> permits NbtByteEditor, NbtDoubleEditor, NbtFloatEditor, NbtGenericEditor, NbtIntEditor, NbtLongEditor, NbtShortEditor, NbtStringEditor {

    private final EditableText labelText = new EditableText();
    @Override
    public int render(String id, ImString label, int flags) {
        super.render(id, label, flags);

        int baseFlags = ImGuiTreeNodeFlags.DrawLinesFull | ImGuiTreeNodeFlags.Leaf;

        int rFlags = 0;

        ImGui.alignTextToFramePadding();
        boolean open = ImGui.treeNodeEx(NbtIcons.getIcon(getNbtType()) + "##" + id, baseFlags);

        if (ImGui.isItemClicked(1)) {
            rFlags |= NbtEditorFlags.RETURN_RIGHT_CLICKED;
        }

        ImGui.sameLine();
        ImGui.beginGroup();

        // label
        boolean canEditLabel = NbtEditorFlags.canEditLabel(flags);
        if (canEditLabel) {
            if (labelText.editString(id, label, ImGui.getFontSize() * 8)) {
                rFlags |= NbtEditorFlags.RETURN_MODIFIED | NbtEditorFlags.RETURN_MODIFIED_LABEL;
            }
        } else {
            ImGui.text(label.get());
        }

        ImGui.sameLine();
        ImGui.text(":");
        ImGui.sameLine();
        if (renderPrimitive(id + ".value", flags)) {
            rFlags |= NbtEditorFlags.RETURN_MODIFIED;
        }

        ImGui.endGroup();
        if (ImGui.isItemClicked(0)) {
            rFlags |= NbtEditorFlags.RETURN_LEFT_CLICKED;
        }
        if (ImGui.isItemClicked(1)) {
            rFlags |= NbtEditorFlags.RETURN_RIGHT_CLICKED;
        }

        if (open) {
            ImGui.treePop();
        }

        return rFlags;
    }

    @Override
    protected void forceEditLabel() {
        labelText.setForceEdit(true);
    }

    protected abstract boolean renderPrimitive(String id, int flags);
}

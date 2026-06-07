package com.igrium.craftui.nbt;

import com.igrium.craftui.icon.NbtIcons;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImString;
import net.minecraft.nbt.NbtElement;

public abstract sealed class NbtPrimitiveEditor<T extends NbtElement> extends NbtEditor<T> permits NbtByteEditor, NbtDoubleEditor, NbtFloatEditor, NbtGenericEditor, NbtIntEditor, NbtLongEditor, NbtShortEditor, NbtStringEditor {

    private final EditableText labelText = new EditableText();
    @Override
    public boolean render(String id, ImString label, int flags) {

        int baseFlags = ImGuiTreeNodeFlags.DrawLinesFull | ImGuiTreeNodeFlags.Leaf;

        ImGui.alignTextToFramePadding();
        boolean open = ImGui.treeNodeEx("##" + id, baseFlags);

        ImGui.sameLine();
        NbtIcons.drawIcon(getNbtType());

        // label
        boolean canEditLabel = NbtEditorFlags.canEditLabel(flags);

        boolean modified = false;

        ImGui.sameLine();
        if (canEditLabel) {
            modified = labelText.editString(id, label, ImGui.getFontSize() * 8);
        } else {
            ImGui.text(label.get());
        }

        ImGui.sameLine();
        ImGui.text(":");
        ImGui.sameLine();
        modified |= renderPrimitive(id + ".value", flags);

        if (open) {
            ImGui.treePop();
        }

        return modified;
    }

    protected abstract boolean renderPrimitive(String id, int flags);

    protected abstract byte getNbtType();
}

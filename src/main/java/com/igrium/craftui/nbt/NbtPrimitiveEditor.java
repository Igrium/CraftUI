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

        int baseFlags = ImGuiTreeNodeFlags.DrawLinesFull | ImGuiTreeNodeFlags.Leaf;

        boolean modified = false;
        boolean modifiedLabel = false;
        boolean leftClicked = false;
        boolean rightClicked = false;

        ImGui.alignTextToFramePadding();
        boolean open = ImGui.treeNodeEx("##" + id, baseFlags);

        ImGui.sameLine();
        ImGui.beginGroup();
        NbtIcons.drawIcon(getNbtType());

        // label
        boolean canEditLabel = NbtEditorFlags.canEditLabel(flags);

        ImGui.sameLine();
        if (canEditLabel) {
            modified = labelText.editString(id, label, ImGui.getFontSize() * 8);
            modifiedLabel = modified;
        } else {
            ImGui.text(label.get());
        }

        ImGui.sameLine();
        ImGui.text(":");
        ImGui.sameLine();
        modified |= renderPrimitive(id + ".value", flags);

        ImGui.endGroup();
        if (ImGui.isItemClicked(0)) {
           leftClicked = true;
        }
        if (ImGui.isItemClicked(1)) {
            rightClicked = true;
        }

        if (open) {
            ImGui.treePop();
        }

        return NbtEditorFlags.getReturnFlags(modified, modifiedLabel, leftClicked, rightClicked);
    }


    protected abstract boolean renderPrimitive(String id, int flags);

    protected abstract byte getNbtType();
}

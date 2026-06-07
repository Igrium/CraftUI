package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImString;
import lombok.NonNull;
import net.minecraft.nbt.NbtElement;

/**
 * A generic NBT "viewer" with no editing capability. Uses toString to render NBT value as a label.
 * @param <T> NBT type
 */
public final class NbtGenericEditor<T extends NbtElement> extends NbtEditor<T> {

    private T value;
    private String valueString = "";

    NbtGenericEditor() {};

    @Override
    public T getNbt() {
        return value;
    }

    @SuppressWarnings("unchecked") // nbt.copy should always return its own type
    @Override
    public void setNbt(@NonNull T nbt) {
        value = (T) nbt.copy();
        valueString = value.toString();
    }

    @Override
    public boolean render(String id, ImString label, int flags) {
        int baseFlags = ImGuiTreeNodeFlags.DrawLinesFull | ImGuiTreeNodeFlags.Leaf;

        boolean open = ImGui.treeNodeEx("##" + id, baseFlags);
        ImGui.sameLine();
        ImGui.text(label.get() + ":");
        ImGui.sameLine();
        ImGui.text(value.toString());

        if (open) {
            ImGui.treePop();
        }

        return false;
    }

}

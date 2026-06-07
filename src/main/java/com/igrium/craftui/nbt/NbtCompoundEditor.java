package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImString;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;

public final class NbtCompoundEditor extends NbtEditor<NbtCompound> {

    private static class Entry {
        final String id;
        final ImString key = new ImString(32);
        NbtEditor<?> value;

        private Entry(String id) {
            this.id = id;
        }

        private Entry(String id, String key, NbtEditor<?> value) {
            this.id = id;
            this.key.set(key);
            this.value = value;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    /**
     * We want to keep entry IDs unique but consistent in the context of adding/removing entries.
     */
    private int curId = 0;

    @Override
    public NbtCompound getNbt() {
        NbtCompound compound = new NbtCompound();
        for (var entry : entries) {
            compound.put(entry.key.get(), entry.value.getNbt());
        }
        return compound;
    }

    @Override
    public void setNbt(NbtCompound nbt) {
        entries.clear();
        for (String key : nbt.getKeys()) {
            entries.add(new Entry("entry." + curId++, key, NbtEditor.of(nbt.get(key))));
        }
    }

    @Override
    public boolean render(String id, ImString label, int flags) {
        boolean canEditLabel = NbtEditorFlags.canEditLabel(flags);
        int baseFlags = ImGuiTreeNodeFlags.DrawLinesFull;
        if (NbtEditorFlags.startsOpen(flags)) {
            baseFlags |= ImGuiTreeNodeFlags.DefaultOpen;
        }

        boolean open = ImGui.treeNodeEx("##" + id, baseFlags);
        ImGui.sameLine();
        ImGui.text(label.get() + " (" + entries.size() + " items)");

        boolean anyModified = false;

        int childFlags = NbtEditorFlags.prepareForChildren(flags);

        if (open) {
            for (var item : entries) {
                anyModified |= item.value.render(item.id, item.key, childFlags);
            }

            ImGui.treePop();
        }

        return anyModified;
    }

}

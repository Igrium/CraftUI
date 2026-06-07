package com.igrium.craftui.nbt;

import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImString;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;

public final class NbtListEditor extends NbtEditor<NbtList> {

    private static class Entry {
        final String id;
        NbtEditor<?> value;

        private Entry(String id) {
            this.id = id;
        }

        private Entry(String id, NbtEditor<?> value) {
            this.id = id;
            this.value = value;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    /**
     * We want to keep entry IDs unique but consistent in the context of adding/removing/reordering entries.
     */
    private int curId = 0;

    @Override
    public NbtList getNbt() {
        NbtList list = new NbtList();
        for (Entry entry : entries) {
            list.add(entry.value.getNbt());
        }
        return list;
    }

    @Override
    public void setNbt(NbtList nbt) {
        entries.clear();
        for (NbtElement item : nbt) {
            entries.add(new Entry("entry." + curId++, NbtEditor.of(item)));
        }
    }

    private final ImString idxString = new ImString(3);

    @Override
    public boolean render(String id, ImString label, int flags) {
        int baseFlags = ImGuiTreeNodeFlags.DrawLinesFull;
        if (NbtEditorFlags.startsOpen(flags)) {
            baseFlags |= ImGuiTreeNodeFlags.DefaultOpen;
        }

        boolean open = ImGui.treeNodeEx("##" + id, baseFlags);
        ImGui.sameLine();
        ImGui.text(label.get() + " (" + entries.size() + " items)");

        boolean anyModified = false;

        int childFlags = NbtEditorFlags.prepareForChildren(flags);
        childFlags |= NbtEditorFlags.READONLY_LABEL;

        if (open) {
            int idx = 0;
            for (var item : entries) {
                idxString.set("[" + idx++ + "]");
                anyModified |= item.value.render(item.id, idxString, childFlags);
            }
            ImGui.treePop();
        }
        return anyModified;
    }
}

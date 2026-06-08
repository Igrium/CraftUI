package com.igrium.craftui.nbt;

import com.igrium.craftui.icon.NbtIcons;
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

    private final EditableText labelText = new EditableText();

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
    public int render(String id, ImString label, int flags) {
        int baseFlags = ImGuiTreeNodeFlags.DrawLinesFull;
        if (NbtEditorFlags.startsOpen(flags)) {
            baseFlags |= ImGuiTreeNodeFlags.DefaultOpen;
        }

        boolean modified = false;
        boolean modifiedLabel = false;
        boolean leftClicked = false;
        boolean rightClicked = false;

        ImGui.alignTextToFramePadding();
        boolean open = ImGui.treeNodeEx("##" + id, baseFlags);

        ImGui.sameLine();
        ImGui.beginGroup();
        NbtIcons.drawIcon(NbtElement.LIST_TYPE);

        boolean canEditLabel = NbtEditorFlags.canEditLabel(flags);
        ImGui.sameLine();
        if (canEditLabel) {
            modified = labelText.editString(id, label, ImGui.getFontSize() * 8);
            modifiedLabel = modified;
        } else {
            ImGui.text(label.get());
        }

        ImGui.sameLine();
        ImGui.text("(" + entries.size() + " items)");

        ImGui.endGroup();
        if (ImGui.isItemClicked(0)) {
            leftClicked = true;
        }
        if (ImGui.isItemClicked(1)) {
            rightClicked = true;
        }

        int childFlags = NbtEditorFlags.prepareForChildren(flags);
        childFlags |= NbtEditorFlags.READONLY_LABEL;

        String removeId = null;

        if (open) {
            int idx = 0;
            for (var item : entries) {
                idxString.set("[" + idx++ + "]");
                int rFlags = item.value.render(item.id, idxString, childFlags);
                modified |= hasFlag(rFlags, NbtEditorFlags.RETURN_MODIFIED);

                // CONTEXT MENU
                if (hasFlag(rFlags, NbtEditorFlags.RETURN_RIGHT_CLICKED)) {
                    ImGui.openPopup(item.id + ".context");
                }
                if (ImGui.beginPopup(item.id + ".context")) {
                    if (ImGui.selectable("Remove")) {
                        removeId = item.id;
                    }
                    ImGui.endPopup();
                }
            }
            ImGui.treePop();
        }

        String r = removeId; // Lambda final restrictions are such bullshit
        if (r != null) {
            modified |= entries.removeIf(e -> e.id.equals(r));
        }

        return NbtEditorFlags.getReturnFlags(modified, modifiedLabel, leftClicked, rightClicked);
    }
}

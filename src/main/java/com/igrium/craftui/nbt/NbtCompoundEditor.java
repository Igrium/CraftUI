package com.igrium.craftui.nbt;

import com.igrium.craftui.icon.NbtIcons;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImString;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

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

    private final EditableText labelText = new EditableText();

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
            //noinspection DataFlowIssue
            entries.add(new Entry("entry." + curId++, key, NbtEditor.of(nbt.get(key))));
        }
    }

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
        NbtIcons.drawIcon(NbtElement.COMPOUND_TYPE);

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

        String removeId = null;

        if (open) {
            for (var item : entries) {
                int rFlags = item.value.render(item.id, item.key, childFlags);
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

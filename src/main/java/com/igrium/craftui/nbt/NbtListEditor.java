package com.igrium.craftui.nbt;

import com.igrium.craftui.icon.NbtIcons;
import com.igrium.craftui.impl.util.NbtTypes;
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

    private boolean openTreeNode = false;

    public void openTreeNode() {
        openTreeNode = true;
    }

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
        super.render(id, label, flags);
        int baseFlags = ImGuiTreeNodeFlags.DrawLinesFull;
        if (NbtEditorFlags.startsOpen(flags)) {
            baseFlags |= ImGuiTreeNodeFlags.DefaultOpen;
        }

        int rFlags = 0;

        ImGui.alignTextToFramePadding();
        if (openTreeNode) ImGui.setNextItemOpen(true);
        boolean open = ImGui.treeNodeEx( NbtIcons.ICON_LIST + "##" + id, baseFlags);

        if (ImGui.isItemClicked(1)) {
            rFlags |= NbtEditorFlags.RETURN_RIGHT_CLICKED;
        }

        ImGui.sameLine();
        ImGui.beginGroup();

        boolean canEditLabel = NbtEditorFlags.canEditLabel(flags);
        if (canEditLabel) {
            if (labelText.editString(id, label, ImGui.getFontSize() * 8)) {
                rFlags |= NbtEditorFlags.RETURN_MODIFIED_LABEL | NbtEditorFlags.RETURN_MODIFIED;
            }
        } else {
            ImGui.text(label.get());
        }

        ImGui.sameLine();
        ImGui.text("(" + entries.size() + " items)");

        ImGui.endGroup();
        if (ImGui.isItemClicked(0)) {
            rFlags |= NbtEditorFlags.RETURN_LEFT_CLICKED;
        }
        if (ImGui.isItemClicked(1)) {
            rFlags |= NbtEditorFlags.RETURN_RIGHT_CLICKED;
        }

        int childFlags = NbtEditorFlags.prepareForChildren(flags);
        childFlags |= NbtEditorFlags.READONLY_LABEL;

        String removeId = null;

        int toMoveUp = -1;
        int toMoveDown = -1;

        if (open) {
            int idx = 0;
            for (var item : entries) {
                idxString.set("[" + idx + "]");
                int cFlags = item.value.render(item.id, idxString, childFlags);

                // Mask out clicks! Only let modification flags bubble up to the parent
                rFlags |= (cFlags & ~(NbtEditorFlags.RETURN_RIGHT_CLICKED | NbtEditorFlags.RETURN_LEFT_CLICKED));

                if (hasFlag(cFlags, NbtEditorFlags.RETURN_RIGHT_CLICKED)) {
                    ImGui.openPopup(item.id + ".context");
                }

                if (ImGui.beginPopup(item.id + ".context")) {

                    int ctxFlags = item.value.drawContextItems(childFlags);
                    rFlags |= (ctxFlags & ~(NbtEditorFlags.RETURN_RIGHT_CLICKED | NbtEditorFlags.RETURN_LEFT_CLICKED));

                    ImGui.separator();
                    ImGui.beginDisabled(hasFlag(flags, NbtEditorFlags.READONLY));
                    if (ImGui.menuItem(t("gui.craftui.nbt_remove"))) {
                        removeId = item.id;
                    }

                    ImGui.beginDisabled(idx <= 0);
                    if (ImGui.menuItem(t("gui.craftui.nbt_moveUp"))) {
                        toMoveUp = idx;
                    }
                    ImGui.endDisabled();

                    ImGui.beginDisabled(idx >= entries.size() - 1);
                    if (ImGui.menuItem(t("gui.craftui.nbt_moveDown"))) {
                        toMoveDown = idx;
                    }
                    ImGui.endDisabled();
                    ImGui.endDisabled();

                    ImGui.endPopup();
                }
                idx++;
            }
            ImGui.treePop();
        }

        String r = removeId; // Lambda final restrictions are such bullshit
        if (r != null && entries.removeIf(e -> e.id.equals(r))) {
            rFlags |= NbtEditorFlags.RETURN_MODIFIED | NbtEditorFlags.RETURN_REMOVED_ITEM;
        }

        if (0 < toMoveUp && toMoveUp < entries.size()) {
            Entry other = entries.get(toMoveUp - 1);
            entries.set(toMoveUp - 1, entries.get(toMoveUp));
            entries.set(toMoveUp, other);
            rFlags |= NbtEditorFlags.RETURN_MODIFIED | NbtEditorFlags.RETURN_REARRANGED;
        }

        if (0 <= toMoveDown && toMoveDown < entries.size() - 1) {
            Entry other = entries.get(toMoveDown + 1);
            entries.set(toMoveDown + 1, entries.get(toMoveDown));
            entries.set(toMoveDown, other);
            rFlags |= NbtEditorFlags.RETURN_MODIFIED | NbtEditorFlags.RETURN_REARRANGED;
        }

        openTreeNode = false;
        return rFlags;
    }

    @Override
    protected Class<? extends NbtList> getNbtClass() {
        return NbtList.class;
    }

    @Override
    protected byte getNbtType() {
        return NbtElement.LIST_TYPE;
    }

    @Override
    protected int drawContextItems(int flags) {
        ImGui.beginDisabled(hasFlag(flags, NbtEditorFlags.READONLY));
        boolean added = false;

        // Can only add items of the same type
        if (entries.isEmpty()) {
            if (ImGui.beginMenu(t("gui.craftui.nbt_addChild"))) {
                byte type = drawTypeChooser();
                if (type > 0) {
                    newItem(type);
                    added = true;
                }
                ImGui.endMenu();
            }
        } else {
            var entry = entries.getFirst();
            byte type = entry.value.getNbtType();
            if (ImGui.menuItem(NbtIcons.getIcon(type) + " " + t("gui.craftui.nbt_addChild"))) {
                newItem(type);
                added = true;
            }
        }

        ImGui.endDisabled();

        int rFlags = added ? NbtEditorFlags.RETURN_MODIFIED | NbtEditorFlags.RETURN_ADDED_ITEM : 0;

        return super.drawContextItems(flags) | rFlags;
    }

    public void newItem(byte type) {
        NbtEditor<?> editor = NbtEditor.of(NbtTypes.createElement(type));
        entries.add(new Entry("entry." + curId++, editor));
        openTreeNode();
    }
}

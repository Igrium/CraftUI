package com.igrium.craftui.nbt;

import com.igrium.craftui.icon.NbtIcons;
import com.igrium.craftui.impl.util.NbtTypes;
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

    private boolean openTreeNode = false;

    public void openTreeNode() {
        openTreeNode = true;
    }

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

        int rFlags = 0;

        ImGui.alignTextToFramePadding();
        if (openTreeNode) ImGui.setNextItemOpen(true);
        boolean open = ImGui.treeNodeEx("##" + id, baseFlags);

        ImGui.sameLine();
        ImGui.beginGroup();
        NbtIcons.drawIcon(NbtElement.COMPOUND_TYPE);

        boolean canEditLabel = NbtEditorFlags.canEditLabel(flags);
        ImGui.sameLine();
        if (canEditLabel) {
            if (labelText.editString(id, label, ImGui.getFontSize() * 8)) {
                rFlags |= NbtEditorFlags.RETURN_MODIFIED | NbtEditorFlags.RETURN_MODIFIED_LABEL;
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

        String removeId = null;

        if (open) {
            for (var item : entries) {
                int cFlags = item.value.render(item.id, item.key, childFlags);

                // Mask out clicks! Only let modification flags bubble up to the parent
                rFlags |= (cFlags & ~(NbtEditorFlags.RETURN_RIGHT_CLICKED | NbtEditorFlags.RETURN_LEFT_CLICKED));

                if (hasFlag(cFlags, NbtEditorFlags.RETURN_RIGHT_CLICKED)) {
                    ImGui.openPopup(item.id + ".context");
                }

                if (ImGui.beginPopup(item.id + ".context")) {

                    int ctxFlags = item.value.drawContextItems(childFlags);
                    rFlags |= (ctxFlags & ~(NbtEditorFlags.RETURN_RIGHT_CLICKED | NbtEditorFlags.RETURN_LEFT_CLICKED));

                    ImGui.separator();
                    if (ImGui.menuItem("Remove")) {
                        removeId = item.id;
                    }
                    ImGui.endPopup();
                }
            }
            ImGui.treePop();
        }

        String r = removeId; // Lambda final restrictions are such bullshit
        if (r != null && entries.removeIf(e -> e.id.equals(r))) {
            rFlags |= NbtEditorFlags.RETURN_MODIFIED | NbtEditorFlags.RETURN_REMOVED_ITEM;
        }

        openTreeNode = false;
        return rFlags;
    }

    @Override
    protected Class<NbtCompound> getNbtClass() {
        return NbtCompound.class;
    }

    @Override
    protected byte getNbtType() {
        return NbtElement.COMPOUND_TYPE;
    }

    @Override
    protected int drawContextItems(int flags) {
        ImGui.beginDisabled(hasFlag(flags, NbtEditorFlags.READONLY));
        boolean added = false;
        if (ImGui.beginMenu(t("gui.craftui.nbt_addChild"))) {
            byte type = drawTypeChooser();
            if (type > 0) {
                newItem(type);
                added = true;
            }
            ImGui.endMenu();
        }
        ImGui.endDisabled();

        int rFlags = added ? NbtEditorFlags.RETURN_MODIFIED | NbtEditorFlags.RETURN_ADDED_ITEM : 0;

        return super.drawContextItems(flags) | rFlags;
    }

    public void newItem(byte type) {
        NbtEditor<?> editor = NbtEditor.of(NbtTypes.createElement(type));
        editor.forceEditLabel();
        entries.add(new Entry("entry." + curId++, NbtIcons.translationSuffix(type), editor));
        openTreeNode();
    }
}

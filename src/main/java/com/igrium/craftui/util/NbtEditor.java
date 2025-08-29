package com.igrium.craftui.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.igrium.craftui.icon.NbtIcons;
import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiSelectableFlags;
import imgui.type.ImString;
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A simple NBT editor that can be used by applications.
 */
@UtilityClass
public final class NbtEditor {

    public static final int READONLY = 1;
    public static final int NO_RENAME = 2;

    /**
     * Render the NBT editor for a given NBT element.
     *
     * @param name  Name of the element
     * @param nbt   The NBT element to edit. <em>Will be directly modified</em>
     * @param flags <code>NbtEditorFlags</code> to use.
     * @return <code>true</code> if the element was modified this frame.
     */
    public static boolean drawNbtEditor(@NonNull String name, @NonNull NbtElement nbt, int flags) {
        return drawNbtEditor(name, nbt, n -> {System.out.println("Renamed to " + n);}, flags);
    }

    private static boolean drawNbtEditor(String name, NbtElement nbt, Consumer<String> renameCallback, int flags) {
        if (nbt instanceof NbtCompound compound) {
            return drawCompoundEditor(name, compound, renameCallback, flags);
        } else {
            ImGui.text("Unable to render NBT element type " + nbt.getType());
            return false;
        }
    }

    /**
     * String IDs of element stack IDs that are currently being renamed.
     */
    private static Map<Integer, ImString> renamingElements = new HashMap<>();
    private static Set<Integer> renameInit = new HashSet<>();

    private static boolean drawCompoundEditor(String name, NbtCompound compound, Consumer<String> renameCallback, int flags) {
        ImGui.alignTextToFramePadding();
        boolean open = ImGui.treeNode("##" + name);
        ImGui.sameLine();
        NbtIcons.drawIcon(NbtElement.COMPOUND_TYPE);
        ImGui.sameLine();

        if (!renamableLabel(name, renameCallback, flags)) {
            ImGui.sameLine();
            ImGui.text("(" + compound.getSize() + " items)");
        };

        boolean modified = false;

        // Delay renames until later to avoid concurrent modification
        BiMap<String, String> renames = HashBiMap.create();

        if (open) {
            for (var key : compound.getKeys()) {
                var value = compound.get(key);
                if (drawNbtEditor(key, value, (newName) -> {
                    // Called if renamed
                    if (newName.equals(key))
                        return;

                    newName = ensureNoDuplicates(newName, n -> compound.contains(n) || renames.containsValue(n));
                    renames.put(key, newName);

                }, flags)) {
                    // Called if value modified
                    modified = true;
                };
            }
            ImGui.treePop();
        }

        for (var entry : renames.entrySet()) {
            modified = true;
            var value = compound.get(entry.getKey());
            compound.remove(entry.getKey());
            compound.put(entry.getValue(), value);
        }

        return modified;
    }

    private static boolean renamableLabel(String name, Consumer<String> renameCallback, int flags) {
        boolean isReadonly = (flags & READONLY) == READONLY;
        boolean noRename = isReadonly || (flags & NO_RENAME) == NO_RENAME;
        int id = ImGui.getID(name);
        ImString renameStr = renamingElements.get(id);

        if (renameStr != null) {
            if (ImGui.inputText("##name", renameStr, ImGuiInputTextFlags.EnterReturnsTrue)) {
                renamingElements.remove(id);
                renameInit.remove(id);
                renameCallback.accept(renameStr.get());
            } else if (renameInit.remove(id)) {
                ImGui.setKeyboardFocusHere(-1);
            } else if (!ImGui.isItemActive()) {
                renamingElements.remove(id);
            }
            return true;
        } else {
            ImGui.selectable(name, false, ImGuiSelectableFlags.AllowDoubleClick);
            if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0) && !noRename) {
                ImString str = new ImString(32);
                str.set(getRenderedText(name));
                renamingElements.put(id, str);
                renameInit.add(id);
            }
            return false;
        }
    }

    private static int findRenderedTextEnd(String text) {
        int hashPos = text.indexOf("##");
        return hashPos >= 0 ? hashPos : text.length();
    }

    private static String getRenderedText(String text) {
        return text.substring(0, findRenderedTextEnd(text));
    }

    private static String ensureNoDuplicates(String str, Predicate<String> contains) {
        int number = 0;
        String result = str;
        while (contains.test(result)) {
            number++;
            result = str + number;
        }
        return result;
    }
}

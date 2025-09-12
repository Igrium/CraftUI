package com.igrium.craftui.impl.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.igrium.craftui.icon.NbtIcons;
import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiSelectableFlags;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImString;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import net.minecraft.nbt.*;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A simple NBT editor that can be used by applications.
 */
@UtilityClass
public final class NbtEditor {

    private static final Logger LOGGER = LoggerFactory.getLogger(NbtEditor.class);

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
        return drawNbtEditor(name, nbt, n -> {System.out.println("Renamed to " + n);}, e -> {}, flags);
    }

    // updateCallback is only called if the actual instance needs to be replaced.
    @SuppressWarnings("unchecked") // T must be of the cast type
    private static <T extends NbtElement> boolean drawNbtEditor(String name, T nbt, Consumer<String> renameCallback,
                                                                Consumer<T> updateCallback, int flags) {
        if (nbt instanceof NbtCompound compound) {
            return drawCompoundEditor(name, compound, renameCallback, flags);
        } else if (nbt instanceof NbtString strNbt) {
            return drawStringEditor(name, strNbt, renameCallback, (Consumer<NbtString>) updateCallback, flags);
        } else if (nbt instanceof NbtByte byteNbt) {
            return drawByteEditor(name, byteNbt, renameCallback, (Consumer<NbtByte>) updateCallback, flags);
        } else if (nbt instanceof NbtShort shortNbt) {
            return drawShortEditor(name, shortNbt, renameCallback, (Consumer<NbtShort>) updateCallback, flags);
        } else if (nbt instanceof NbtInt intNbt) {
            return drawIntEditor(name, intNbt, renameCallback, (Consumer<NbtInt>) updateCallback, flags);
        } else if (nbt instanceof NbtLong longNbt) {
            return drawLongEditor(name, longNbt, renameCallback, (Consumer<NbtLong>) updateCallback, flags);
        } else if (nbt instanceof NbtFloat floatNbt) {
            return drawFloatEditor(name, floatNbt, renameCallback, (Consumer<NbtFloat>) updateCallback, flags);
        } else if (nbt instanceof NbtDouble doubleNbt) {
            return drawDoubleEditor(name, doubleNbt, renameCallback, (Consumer<NbtDouble>) updateCallback, flags);
        } else {
            return drawUnknown(name, nbt, renameCallback, flags);
        }
    }

    /**
     * String IDs of element stack IDs that are currently being renamed.
     */
    private static final Map<Integer, ImString> renamingElements = new HashMap<>();
    private static final Set<Integer> renameInit = new HashSet<>();

    private static boolean drawCompoundEditor(String name, NbtCompound compound, Consumer<String> renameCallback, int flags) {
        ImGui.alignTextToFramePadding();
        boolean open = ImGui.treeNodeEx("##" + name);
        ImGui.sameLine();
        NbtIcons.drawIcon(NbtElement.COMPOUND_TYPE);
        ImGui.sameLine();

        if (!renamableLabel(name, renameCallback, flags)) {
            ImGui.sameLine();
            ImGui.text("(" + compound.getSize() + " items)");
        };

        // Delay renames until later to avoid concurrent modification
        BiMap<String, String> renames = HashBiMap.create();
        MutableBoolean updated = new MutableBoolean();

        if (open) {
            // Copy to avoid concurrent modification when replacing values
            List<String> keyList = new ArrayList<>(compound.getKeys());
            keyList.sort(String.CASE_INSENSITIVE_ORDER); // Order is inconsistent anyway; might as well make alphabetical

            for (var key : keyList) {
                var value = compound.get(key);
                if (drawNbtEditor(key, value, (newName) -> {
                    // Called if renamed
                    if (newName.equals(key))
                        return;

                    newName = ensureNoDuplicates(newName, n -> compound.contains(n) || renames.containsValue(n));
                    renames.put(key, newName);
                    updated.setTrue();

                }, (newVal) -> {
                    // Called if value updated.
                    compound.put(key, newVal);
                    updated.setTrue();
                }, flags &= ~NO_RENAME)) { // Allow to be renamed even if this compound cannot be renamed
                    // Could also be called if value updated
                    updated.setTrue();
                };
            }
            ImGui.treePop();
        }

        for (var entry : renames.entrySet()) {
            var value = compound.get(entry.getKey());
            compound.remove(entry.getKey());
            compound.put(entry.getValue(), value);
        }

        return updated.booleanValue();
    }

    private static boolean drawStringEditor(String name, NbtString nbt, Consumer<String> renameCallback, Consumer<NbtString> updateCallback, int flags) {
        return drawPrimitiveEditor(name, nbt, nbt.asString(),
                renameCallback, str -> updateCallback.accept(NbtString.of(str)), flags);
    }

    private static boolean drawByteEditor(String name, NbtByte nbt, Consumer<String> renameCallback, Consumer<NbtByte> updateCallback, int flags) {
        return drawPrimitiveEditor(name, nbt, Byte.toString(nbt.byteValue()),
                renameCallback, str -> updateCallback.accept(NbtByte.of(Byte.parseByte(str))), flags);
    }

    private static boolean drawShortEditor(String name, NbtShort nbt, Consumer<String> renameCallback, Consumer<NbtShort> updateCallback, int flags) {
        return drawPrimitiveEditor(name, nbt, Short.toString(nbt.shortValue()),
                renameCallback, str -> updateCallback.accept(NbtShort.of(Short.parseShort(str))), flags);
    }

    private static boolean drawIntEditor(String name, NbtInt nbt, Consumer<String> renameCallback, Consumer<NbtInt> updateCallback, int flags) {
        return drawPrimitiveEditor(name, nbt, Integer.toString(nbt.intValue()),
                renameCallback, str -> updateCallback.accept(NbtInt.of(Integer.parseInt(str))), flags);
    }

    private static boolean drawLongEditor(String name, NbtLong nbt, Consumer<String> renameCallback, Consumer<NbtLong> updateCallback, int flags) {
        return drawPrimitiveEditor(name, nbt, Long.toString(nbt.longValue()),
                renameCallback, str -> updateCallback.accept(NbtLong.of(Long.parseLong(str))), flags);
    }

    private static boolean drawFloatEditor(String name, NbtFloat nbt, Consumer<String> renameCallback, Consumer<NbtFloat> updateCallback, int flags) {
        return drawPrimitiveEditor(name, nbt, Float.toString(nbt.floatValue()),
                renameCallback, str -> updateCallback.accept(NbtFloat.of(Float.parseFloat(str))), flags);
    }

    private static boolean drawDoubleEditor(String name, NbtDouble nbt, Consumer<String> renameCallback, Consumer<NbtDouble> updateCallback, int flags) {
        return drawPrimitiveEditor(name, nbt, Double.toString(nbt.doubleValue()),
                renameCallback, str -> updateCallback.accept(NbtDouble.of(Double.parseDouble(str))), flags);
    }

    private static <T extends NbtElement> boolean drawPrimitiveEditor(
            String name, T element, String elementStringValue,
            Consumer<String> renameCallback, Consumer<String> updateCallback, int flags) {

        MutableBoolean updated = new MutableBoolean();

        ImGui.alignTextToFramePadding();
        boolean open = ImGui.treeNodeEx("##name" + name, ImGuiTreeNodeFlags.Leaf);

        ImGui.sameLine();
        NbtIcons.drawIcon(element.getType());

        ImGui.sameLine(0, 4);
        renamableLabel(name + "##name", str -> {
            updated.setTrue();
            renameCallback.accept(str);
        }, flags);

        ImGui.sameLine();
        ImGui.text(":");

        ImGui.sameLine();
        renamableLabel(elementStringValue + "##value", str -> {
            updated.setTrue();
            try {
                updateCallback.accept(str);
            } catch (NumberFormatException e) {
                LOGGER.error("Invalid value for NBT number: {}", str);
            }
        }, flags &~NO_RENAME); // This isn't the name, so remove no_rename

        if (open) {
            ImGui.treePop();
        }

        return updated.booleanValue();
    }

    private static boolean drawUnknown(String name, NbtElement element, Consumer<String> renameCallback, int flags) {
        ImGui.alignTextToFramePadding();
        boolean open = ImGui.treeNodeEx("##" + name, ImGuiTreeNodeFlags.Leaf);
        ImGui.sameLine();
        NbtIcons.drawIcon(element.getType());

        MutableBoolean updated = new MutableBoolean();

        ImGui.sameLine();
        renamableLabel(name, str -> {
            updated.setTrue();
            renameCallback.accept(str);
        }, flags);

        ImGui.sameLine();
        ImGui.text(" [NBT type not supported]");

        if (open) {
            ImGui.treePop();
        }

        return updated.booleanValue();
    }

    private static boolean renamableLabel(String name, Consumer<String> renameCallback, int flags) {
        boolean isReadonly = (flags & READONLY) == READONLY;
        boolean noRename = isReadonly || (flags & NO_RENAME) == NO_RENAME;
        int id = ImGui.getID(name);
        ImString renameStr = renamingElements.get(id);

        if (renameStr != null) {
            if (ImGui.inputText("##label", renameStr, ImGuiInputTextFlags.EnterReturnsTrue)) {
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
            float labelWidth = ImGui.calcTextSize(getRenderedText(name)).x;

            ImGui.selectable(name, false, ImGuiSelectableFlags.AllowDoubleClick, labelWidth, 0);
            if (ImGui.isItemHovered() && ImGui.isMouseDoubleClicked(0) && !noRename) {
                ImString str = new ImString();
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
            result = str + "." + number;
        }
        return result;
    }
}

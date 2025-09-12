package com.igrium.craftui.style;

import com.igrium.craftui.impl.style.LayoutManager;
import lombok.experimental.UtilityClass;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Contains various functions for controlling the layout of CraftUI
 */
@UtilityClass
public class CraftUILayouts {

    public static final Identifier DEFAULT = Identifier.of("craftui:default");

    /**
     * Get the layout ID that is currently in use.
     */
    public static @NotNull Identifier getActiveLayout() {
        return LayoutManager.getInstance().getActiveLayout();
    }

    /**
     * Switch to a specific layout.
     *
     * @param layoutId Layout identifier to use; <code>null</code> to switch to the default.
     */
    public static void setActiveLayout(@Nullable Identifier layoutId) {
        LayoutManager.getInstance().setActiveLayout(layoutId);
    }

    /**
     * Get the layout data for the active layout.
     *
     * @return Serialized IMGUI layout data; an empty string if the active layout ID was set to a non-existent layout.
     */
    public static @NotNull String getActiveLayoutData() {
        return LayoutManager.getInstance().getActiveLayoutData();
    }

    /**
     * Obtain the data for a layout as defined by a resourcepack.
     *
     * @param id Layout identifier.
     * @return The serialized IMGUI layout data;
     * <code>null</code> if no resourcepack provides a layout with that ID.
     */
    public @Nullable String getNativeLayoutData(Identifier id) {
        return LayoutManager.getInstance().getNativeLayoutData(id);
    }

    /**
     * Obtain the data for a layout as defined by a user override;
     *
     * @param id Layout identifier.
     * @return The serialized IMGUI layout data;
     * <code>null</code> if the user hasn't written any custom layout data with that ID.
     */
    public @Nullable String getUserLayoutData(Identifier id) {
        return LayoutManager.getInstance().getUserLayoutData(id);
    }

    /**
     * Get the layout data for a given ID. If the user has overwritten this layout, use that.
     * Otherwise, use the resourcepack-provided data.
     *
     * @param id Layout identifier.
     * @return The serialized IMGUI layout data;
     * <code>null</code> if the user hasn't written any layout data with that ID and no resourcepack defines it.
     */
    public @Nullable String getLayoutData(Identifier id) {
        return LayoutManager.getInstance().getLayoutData(id);
    }

    /**
     * Save custom layout data for a given ID. Save to disk if layout persistence is enabled.
     *
     * @param id   Layout identifier.
     * @param data Serialized IMGUI layout data to save.
     */
    public void setUserLayoutData(Identifier id, String data) {
        LayoutManager.getInstance().setUserLayoutData(id, data, true);
    }

    /**
     * Save updated user layout data to the currently active layout on disk.
     *
     * @param data Serialized IMGUI layout data.
     * @implNote Only saves to memory if persistent layout is disabled.
     */
    public void saveUserLayoutData(String data) {
        LayoutManager.getInstance().saveUserLayoutData(data);
    }

    /**
     * Clear all user overrides from a layout, restoring it to the version provided by the resourcepack.
     *
     * @param id Layout identifier.
     */
    public void resetLayout(Identifier id) {
        LayoutManager.getInstance().resetLayout(id);
    }

    /**
     * Clear all user layout data, restoring all layouts to the versions provided by the resourcepack.
     */
    public void resetLayouts() {
        LayoutManager.getInstance().resetLayouts();
    }

    /**
     * Signal IMGUI to reparse the data from the layout manager.
     * Call after updating user data for the active layout.
     */
    public void requestLayoutUpdate() {
        LayoutManager.getInstance().setLayoutUpdate(true);
    }

}

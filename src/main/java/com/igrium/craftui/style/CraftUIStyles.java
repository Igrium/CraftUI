package com.igrium.craftui.style;

import com.igrium.craftui.impl.style.StyleManager;
import lombok.NonNull;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class CraftUIStyles {
    /**
     * The default, "dark" style
     */
    public static final Identifier DARK = Identifier.of("craftui:dark");

    /**
     * Find a loaded style from its identifier.
     * @param id Style ID to search for.
     * @return The style, or <code>null</code> if no style by that ID is loaded.
     */
    public static @Nullable CraftUIStyle getStyle(Identifier id) {
        return StyleManager.getInstance().getStyle(id);
    }

    /**
     * Get the currently-loaded style.
     * @return Current style identifier.
     */
    public @NonNull Identifier getActiveStyle() {
        return StyleManager.getInstance().getActiveStyle();
    }

    /**
     * Switch the UI to a new style.
     * @param id Style ID to switch to.
     */
    public void setActiveStyle(@NonNull Identifier id) {
        StyleManager.getInstance().setActiveStyle(id);
    }

    /**
     * Push a style update to the UI. Use after manually editing the active style.
     */
    public void updateStyle() {
        StyleManager.getInstance().setWantStyleUpdate(true);
    }
}

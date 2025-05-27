package com.igrium.craftui.input;

import com.igrium.craftui.app.AppManager;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Handles viewport controls when the user clicks on the viewport. Useful to avoid having to press T to access the mouse.
 */
public class ViewportController {

    /**
     * If set, called every frame to check if we should activate.
     * If unset, setActive must be called manually.
     */
    @Nullable
    @Getter @Setter
    private BooleanSupplier activationPredicate;


    @Getter @Setter
    private boolean active;

    public final void onTick() {
        if (activationPredicate != null) {
            setActive(activationPredicate.getAsBoolean());
        }

        if (active) {
            update();
        } else {
            AppManager.forceMouseUnlock();
        }
    }

    /**
     * Called every frame while the viewport controller is active.
     */
    protected void update() {

    }
}

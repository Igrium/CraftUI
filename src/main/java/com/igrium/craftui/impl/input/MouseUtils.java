package com.igrium.craftui.impl.input;

import com.igrium.craftui.app.CraftApp.ViewportBounds;
import com.mojang.blaze3d.platform.Window;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2d;

public class MouseUtils {

    /**
     * Whether the minecraft client thinks the mouse is pressed.
     * Respects ImGui event consumption, but <em>not</em> vanilla screen consumption.
     */
    @Getter @Setter
    private static boolean mousePressed;

    public static Vector2d calculateViewportMouse(Window window, ViewportBounds viewport, double globalX, double globalY) {
        // Isn't OpenGL flipped y-axis fun??
        double yOffset = window.getScreenHeight() - viewport.height() - viewport.y();

        double xScale = window.getScreenWidth() / (double) viewport.width();
        double yScale = window.getScreenHeight() / (double) viewport.height();

        return new Vector2d((globalX - viewport.x()) * xScale, (globalY - yOffset) * yScale);
    }
}

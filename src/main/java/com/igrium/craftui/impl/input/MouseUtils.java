package com.igrium.craftui.impl.input;

import com.igrium.craftui.api.app.CraftApp.ViewportBounds;
import com.mojang.blaze3d.platform.Window;
import org.joml.Vector2d;

public class MouseUtils {
    public static Vector2d calculateViewportMouse(Window window, ViewportBounds viewport, double globalX, double globalY) {
        // Isn't OpenGL flipped y-axis fun??
        double yOffset = window.getScreenHeight() - viewport.height() - viewport.y();

        double xScale = window.getScreenWidth() / (double) viewport.width();
        double yScale = window.getScreenHeight() / (double) viewport.height();

        return new Vector2d((globalX - viewport.x()) * xScale, (globalY - yOffset) * yScale);
    }
}

package com.igrium.craftui.input;

import com.igrium.craftui.app.CraftApp.ViewportBounds;

import net.minecraft.client.MinecraftClient;

public class MouseUtils {
    public static record MousePos(double x, double y) {

    }

    public static MousePos calculateViewportMouse(MinecraftClient client, ViewportBounds viewport, double globalX, double globalY) {
        client.getWindow().getWidth();
        // Isn't OpenGL flipped y-axis fun??
        double yOffset = client.getWindow().getHeight() - viewport.height() - viewport.y();

        double xScale = client.getWindow().getWidth() / (double) viewport.width();
        double yScale = client.getWindow().getHeight() / (double) viewport.height();

        return new MousePos((globalX - viewport.x()) * xScale, (globalY - yOffset) * yScale);
    }
}

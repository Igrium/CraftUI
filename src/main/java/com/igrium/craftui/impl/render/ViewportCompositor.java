package com.igrium.craftui.impl.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

/**
 * Owns the full-window composite texture used to confine the 3D world to a docked viewport.
 *
 * <p>On Minecraft 26.2 the world, the vanilla GUI and ImGui all render into a single
 * window-sized {@code mainRenderTarget} that is presented with one region-less
 * {@link com.mojang.blaze3d.systems.GpuSurface#blitFromTexture} call — there is no GL viewport
 * call and no scaled/offset blit. To confine the world we shrink the game render target to the
 * sub-rectangle's size (via the window-size override) and then, each frame, draw that
 * frame-sized image into the correct sub-rectangle of this full-window composite texture (see
 * {@link ImGuiImplBlaze3D#renderGameToComposite}). ImGui is drawn on top of the composite and
 * the composite is what gets presented. This mirrors the compositing step of Moulberry's
 * Flashback viewport subsystem.
 */
public class ViewportCompositor {

    private GpuTexture texture;
    private GpuTextureView view;
    private int width;
    private int height;

    /** The full-window composite texture view, or {@code null} if not yet allocated. */
    public GpuTextureView getView() {
        return view;
    }

    /** (Re)allocate the composite texture so it matches the given full-window framebuffer size. */
    public void ensureSize(int fbWidth, int fbHeight, GpuFormat format) {
        if (texture != null && width == fbWidth && height == fbHeight && texture.getFormat() == format) {
            return;
        }
        close();
        texture = RenderSystem.getDevice().createTexture(
                () -> "CraftUI Viewport Composite",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_COPY_SRC
                        | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
                format, fbWidth, fbHeight, 1, 1);
        view = RenderSystem.getDevice().createTextureView(texture);
        width = fbWidth;
        height = fbHeight;
    }

    public void close() {
        if (view != null) {
            view.close();
            view = null;
        }
        if (texture != null) {
            texture.close();
            texture = null;
        }
        width = 0;
        height = 0;
    }
}

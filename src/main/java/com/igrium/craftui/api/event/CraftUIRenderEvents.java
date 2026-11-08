package com.igrium.craftui.api.event;

import com.igrium.craftui.api.app.CraftApp;
import com.igrium.craftui.api.app.CraftApp.ViewportBounds;
import com.igrium.craftui.api.style.CraftUIStyle;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Various hooks into CraftUI's render cycle.
 * <p>These are quite powerful, and could mess up other mods. Use with caution.</p>
 *
 * @apiNote While effort is taken to ensure these hooks are called in the right order, their exact placement may vary
 * between minor versions.
 */
@UtilityClass
public final class CraftUIRenderEvents {

    /**
     * Called at the beginning of CraftUI's "pre-render" phase, after viewport bounds are updated but before app
     * pre-render functions are called.
     *
     * @apiNote Called even if there are no apps open
     */
    public static final Event<PreRender> PRE_RENDER = EventFactory.createArrayBacked(
            PreRender.class, listeners -> apps -> {
                for (var l : listeners) {
                    l.preRender(apps);
                }
            }
    );

    /**
     * Called after CraftUI computes the viewport bounds for all open apps.
     * May directly override viewport bounds computed from apps. Use with caution.
     */
    public static final Event<ComputeViewportBounds> COMPUTE_VIEWPORT_BOUNDS = EventFactory.createArrayBacked(
            ComputeViewportBounds.class, listeners -> bounds -> {
                for (var l : listeners) {
                    bounds = l.computeViewportBounds(bounds);
                }
                return bounds;
            }
    );

    /**
     * Called when the ImGui style must be updated for whatever reason
     *
     * @apiNote The mutable style supplied is temporary; it will not affect the stored values.
     */
    public static final Event<UpdateStyle> UPDATE_STYLE = EventFactory.createArrayBacked(
            UpdateStyle.class, listeners -> (ctx, style) -> {
                for (var l : listeners) {
                    l.updateStyle(ctx, style);
                }
            }
    );

    /**
     * Called at the very beginning of CraftUI's render cycle. Must not make ImGui calls.
     */
    public static final Event<BeginFrame> BEGIN_FRAME = EventFactory.createArrayBacked(
            BeginFrame.class, listeners -> ctx -> {
                for (var l : listeners) {
                    l.beginFrame(ctx);
                }
            }
    );

    /**
     * Called at the very end of CraftUI's render cycle. Must not make ImGui calls.
     */
    public static final Event<EndFrame> END_FRAME = EventFactory.createArrayBacked(
            EndFrame.class, listeners -> ctx -> {
                for (var l : listeners) {
                    l.endFrame(ctx);
                }
            }
    );

    /**
     * Called directly after <code>ImGui.newFrame</code> and <code>ImGuizmo.beginFrame</code>, as soon as ImGui is
     * ready to receive draw commands.
     */
    public static final Event<BeginRender> BEGIN_RENDER = EventFactory.createArrayBacked(
            BeginRender.class, listeners -> ctx -> {
                for (var l : listeners) {
                    l.beginRender(ctx);
                }
            }
    );

    /**
     * Called at the end of the UI draw cycle, just before the ImGui draw data is finalized.
     */

    public static final Event<EndRender> END_RENDER = EventFactory.createArrayBacked(
            EndRender.class, listeners -> ctx -> {
                for (var l : listeners) {
                    l.endRender(ctx);
                }
            }
    );

    @FunctionalInterface
    public interface PreRender {
        void preRender(Collection<? extends CraftApp> apps);
    }

    @FunctionalInterface
    public interface ComputeViewportBounds {
        @Nullable ViewportBounds computeViewportBounds(@Nullable ViewportBounds bounds);
    }

    @FunctionalInterface
    public interface UpdateStyle {
        void updateStyle(CraftUIRenderContext ctx, CraftUIStyle style);
    }

    @FunctionalInterface
    public interface BeginFrame {
        void beginFrame(CraftUIRenderContext ctx);
    }

    @FunctionalInterface
    public interface EndFrame {
        void endFrame(CraftUIRenderContext ctx);
    }

    @FunctionalInterface
    public interface BeginRender {
        void beginRender(CraftUIRenderContext ctx);
    }

    @FunctionalInterface
    public interface EndRender {
        void endRender(CraftUIRenderContext ctx);
    }
}

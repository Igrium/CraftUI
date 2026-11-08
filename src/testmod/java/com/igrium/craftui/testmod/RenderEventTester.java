package com.igrium.craftui.testmod;

import com.igrium.craftui.api.app.CraftApp.ViewportBounds;
import com.igrium.craftui.api.event.CraftUIRenderEvents;
import com.igrium.craftui.api.event.ctx.CraftUIRenderContext;
import com.igrium.craftui.api.style.CraftUIStyle;
import com.igrium.craftui.api.style.CraftUIStyles;
import com.mojang.blaze3d.platform.Window;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Exercises {@link CraftUIRenderEvents}. Only behavior documented on the events is judged pass/fail;
 * anything else (exact placement, call frequency) is shown for information only.
 *
 * @implNote I've been vibe-coding the test app cause the code really doesn't matter
 */
public final class RenderEventTester {

    private RenderEventTester() {}

    private enum Ev {
        PRE_RENDER, COMPUTE_VIEWPORT_BOUNDS, UPDATE_STYLE, POST_COMPOSITE, BEGIN_FRAME, END_FRAME,
        BEFORE_RENDER, BEGIN_RENDER, END_RENDER, AFTER_RENDER;

        /** Pre-render events are outside the "render cycle" bracketed by BEGIN_FRAME / END_FRAME. */
        boolean isPreRender() {
            return this == PRE_RENDER || this == COMPUTE_VIEWPORT_BOUNDS;
        }
    }

    private static final Map<Ev, Long> callCounts = new EnumMap<>(Ev.class);

    // Context snapshot (from BEGIN_FRAME)
    private static int lastAppCount;
    private static boolean lastCleanupFrame;
    private static @Nullable ViewportBounds lastCtxBounds;

    // Documented: "PRE_RENDER is called even if there are no apps open"
    private static long preRenderWithoutApps;
    // Documented: "POST_COMPOSITE is only called if there are custom viewport bounds active"
    private static long postCompositeWithoutBounds;

    // Frame capture
    private static boolean captureRequested;
    private static boolean capturing;
    private static final List<Ev> captureBuffer = new ArrayList<>();
    private static @Nullable List<Ev> capturedOrder;
    private static boolean capturedHadBounds;

    // COMPUTE_VIEWPORT_BOUNDS override
    private static final String[] BOUNDS_MODES = {"Pass-through", "Force null", "Force inset"};
    private static final ImInt boundsMode = new ImInt(0);
    private static final int[] boundsInset = {64};
    private static @Nullable ViewportBounds lastIncomingBounds;
    private static @Nullable ViewportBounds lastReturnedBounds;

    // UPDATE_STYLE override
    private static final ImBoolean overrideStyle = new ImBoolean(false);
    private static final float[] overrideRounding = {12f};
    private static boolean styleWasSameInstance;
    private static float storedRoundingAtUpdate = Float.NaN;

    // Draw tests
    private static final ImBoolean drawOverlay = new ImBoolean(false);
    private static final ImBoolean drawBoundsOutline = new ImBoolean(false);

    private static void record(Ev ev) {
        callCounts.merge(ev, 1L, Long::sum);
        if (capturing) {
            captureBuffer.add(ev);
        }
    }

    public static void register() {
        CraftUIRenderEvents.PRE_RENDER.register(ctx -> {
            record(Ev.PRE_RENDER);
            if (ctx.getApps().isEmpty()) preRenderWithoutApps++;
        });

        CraftUIRenderEvents.COMPUTE_VIEWPORT_BOUNDS.register((ctx, bounds) -> {
            record(Ev.COMPUTE_VIEWPORT_BOUNDS);
            lastIncomingBounds = bounds;
            ViewportBounds result = switch (boundsMode.get()) {
                case 1 -> null;
                case 2 -> {
                    Window window = ctx.getClient().getWindow();
                    int inset = boundsInset[0];
                    int w = Math.max(window.getScreenWidth() - inset * 2, 1);
                    int h = Math.max(window.getScreenHeight() - inset * 2, 1);
                    yield new ViewportBounds(inset, inset, w, h);
                }
                default -> bounds;
            };
            lastReturnedBounds = result;
            return result;
        });

        CraftUIRenderEvents.UPDATE_STYLE.register((ctx, style) -> {
            record(Ev.UPDATE_STYLE);
            CraftUIStyle stored = storedStyle();
            styleWasSameInstance = style == stored;
            storedRoundingAtUpdate = stored != null ? stored.getWindowRounding() : Float.NaN;
            if (overrideStyle.get()) {
                style.setWindowRounding(overrideRounding[0]);
            }
        });

        CraftUIRenderEvents.POST_COMPOSITE.register(ctx -> {
            record(Ev.POST_COMPOSITE);
            if (ctx.getViewportBounds() == null) postCompositeWithoutBounds++;
        });

        CraftUIRenderEvents.BEGIN_FRAME.register(ctx -> {
            record(Ev.BEGIN_FRAME);
            lastAppCount = ctx.getApps().size();
            lastCleanupFrame = ctx.isCleanupFrame();
            lastCtxBounds = ctx.getViewportBounds();
            if (capturing) capturedHadBounds = ctx.getViewportBounds() != null;
        });

        CraftUIRenderEvents.BEFORE_RENDER.register(ctx -> record(Ev.BEFORE_RENDER));

        CraftUIRenderEvents.BEGIN_RENDER.register(ctx -> {
            record(Ev.BEGIN_RENDER);
            if (drawOverlay.get() && !ctx.getApps().isEmpty()) {
                drawOverlayWindow(ctx);
            }
        });

        CraftUIRenderEvents.END_RENDER.register(ctx -> {
            record(Ev.END_RENDER);
            if (drawBoundsOutline.get() && !ctx.getApps().isEmpty()) {
                drawOutline(ctx);
            }
        });

        CraftUIRenderEvents.AFTER_RENDER.register(ctx -> record(Ev.AFTER_RENDER));

        CraftUIRenderEvents.END_FRAME.register(ctx -> {
            record(Ev.END_FRAME);
            // A capture spans END_FRAME (exclusive) -> next END_FRAME (inclusive), so it includes
            // the pre-render events wherever they happen to fall.
            if (capturing) {
                capturing = false;
                capturedOrder = List.copyOf(captureBuffer);
                TestmodClient.LOGGER.info("Captured CraftUI render event order: {}", capturedOrder);
            } else if (captureRequested) {
                captureRequested = false;
                capturing = true;
                captureBuffer.clear();
                capturedHadBounds = false;
            }
        });
    }

    private static @Nullable CraftUIStyle storedStyle() {
        return CraftUIStyles.getStyle(CraftUIStyles.getActiveStyle());
    }

    private static void drawOverlayWindow(CraftUIRenderContext ctx) {
        ImGui.setNextWindowPos(16, 16, ImGuiCond.FirstUseEver);
        ImGui.setNextWindowBgAlpha(0.6f);
        if (ImGui.begin("Render Event Overlay", ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoSavedSettings)) {
            ImGui.text("Drawn from BEGIN_RENDER");
            ImGui.text("BEGIN_RENDER calls: " + callCounts.getOrDefault(Ev.BEGIN_RENDER, 0L));
            ImGui.text("Cleanup frame: " + ctx.isCleanupFrame());
        }
        ImGui.end();
    }

    private static void drawOutline(CraftUIRenderContext ctx) {
        ViewportBounds b = ctx.getViewportBounds();
        if (b == null) return;
        // ViewportBounds uses a bottom-left origin; ImGui uses top-left.
        float displayH = ImGui.getIO().getDisplaySizeY();
        float top = displayH - b.y() - b.height();
        ImGui.getForegroundDrawList().addRect(b.x(), top, b.x() + b.width(), top + b.height(),
                ImGui.colorConvertFloat4ToU32(1, 0, 1, 1), 0, 0, 3);
        ImGui.getForegroundDrawList().addText(b.x() + 4, top + 4,
                ImGui.colorConvertFloat4ToU32(1, 0, 1, 1), "END_RENDER outline");
    }

    // ---- UI ----

    public static void renderWindow() {
        if (ImGui.begin("Render Events")) {
            if (ImGui.collapsingHeader("Call Counts")) {
                if (ImGui.beginTable("##counts", 2, ImGuiTableFlags.Borders | ImGuiTableFlags.RowBg)) {
                    for (Ev ev : Ev.values()) {
                        ImGui.tableNextRow();
                        ImGui.tableNextColumn();
                        ImGui.text(ev.name());
                        ImGui.tableNextColumn();
                        ImGui.text(Long.toString(callCounts.getOrDefault(ev, 0L)));
                    }
                    ImGui.endTable();
                }
                if (ImGui.button("Reset Counters")) {
                    callCounts.clear();
                    preRenderWithoutApps = 0;
                    postCompositeWithoutBounds = 0;
                }
                ImGui.separator();
                ImGui.text("Last context: apps=" + lastAppCount + ", cleanup=" + lastCleanupFrame);
                ImGui.text("Context bounds: " + lastCtxBounds);
            }

            if (ImGui.collapsingHeader("Documented Behavior")) {
                // Counts since last reset; reopen the app after /apptest close to check.
                check("PRE_RENDER called with no apps open (" + preRenderWithoutApps + ")",
                        preRenderWithoutApps > 0 ? Boolean.TRUE : null);
                check("POST_COMPOSITE never called without viewport bounds ("
                        + postCompositeWithoutBounds + " violations)", postCompositeWithoutBounds == 0);
            }

            if (ImGui.collapsingHeader("Frame Order")) {
                if (ImGui.button(captureRequested || capturing ? "Capturing..." : "Capture Next Frame")) {
                    captureRequested = true;
                }
                List<Ev> order = capturedOrder;
                if (order != null) {
                    renderOrderChecks(order);
                    ImGui.separator();
                    ImGui.textDisabled("Raw order (placement not covered above is undefined):");
                    for (int i = 0; i < order.size(); i++) {
                        ImGui.text(i + ": " + order.get(i));
                    }
                }
            }

            if (ImGui.collapsingHeader("COMPUTE_VIEWPORT_BOUNDS")) {
                ImGui.combo("Mode##bounds", boundsMode, BOUNDS_MODES);
                if (boundsMode.get() == 2) {
                    ImGui.sliderInt("Inset", boundsInset, 0, 512);
                }
                ImGui.text("Incoming: " + lastIncomingBounds);
                ImGui.text("Returned: " + lastReturnedBounds);
                ImGui.text("Seen by render ctx: " + lastCtxBounds);
                check("Render context reflects returned bounds", sameBounds(lastReturnedBounds, lastCtxBounds));
            }

            if (ImGui.collapsingHeader("UPDATE_STYLE")) {
                boolean changed = ImGui.checkbox("Override Window Rounding", overrideStyle);
                if (overrideStyle.get()) {
                    changed |= ImGui.sliderFloat("Rounding", overrideRounding, 0, 24);
                }
                changed |= ImGui.button("Force Style Update");

                if (changed) {
                    CraftUIStyles.updateStyle();
                }

                CraftUIStyle stored = storedStyle();
                float storedRounding = stored != null ? stored.getWindowRounding() : Float.NaN;
                ImGui.text("Live ImGui rounding: " + ImGui.getStyle().getWindowRounding());
                ImGui.text("Stored style rounding: " + storedRounding);
                if (callCounts.getOrDefault(Ev.UPDATE_STYLE, 0L) > 0) {
                    // Documented: "The mutable style supplied is temporary; it will not affect the stored values."
                    check("Listener got a temporary style, not the stored one", !styleWasSameInstance);
                    check("Stored style unchanged by listener edits",
                            Float.compare(storedRounding, storedRoundingAtUpdate) == 0);
                }
            }

            if (ImGui.collapsingHeader("Draw Tests")) {
                ImGui.checkbox("Overlay window (BEGIN_RENDER)", drawOverlay);
                ImGui.checkbox("Viewport outline (END_RENDER)", drawBoundsOutline);
            }
        }
        ImGui.end();
    }

    private static void renderOrderChecks(List<Ev> order) {
        List<Ev> renderCycle = order.stream().filter(e -> !e.isPreRender()).toList();
        check("BEGIN_FRAME is the first render-cycle event",
                !renderCycle.isEmpty() && renderCycle.getFirst() == Ev.BEGIN_FRAME);
        check("END_FRAME is the last render-cycle event",
                !renderCycle.isEmpty() && renderCycle.getLast() == Ev.END_FRAME);

        boolean drew = order.contains(Ev.BEGIN_RENDER);
        if (drew) {
            check("BEFORE_RENDER before BEGIN_RENDER", before(order, Ev.BEFORE_RENDER, Ev.BEGIN_RENDER));
            check("BEGIN_RENDER before END_RENDER", before(order, Ev.BEGIN_RENDER, Ev.END_RENDER));
            check("END_RENDER before AFTER_RENDER", before(order, Ev.END_RENDER, Ev.AFTER_RENDER));
        } else {
            ImGui.textDisabled("(no draw this frame; render ordering not checked)");
        }

        if (order.contains(Ev.PRE_RENDER)) {
            check("COMPUTE_VIEWPORT_BOUNDS before PRE_RENDER",
                    before(order, Ev.COMPUTE_VIEWPORT_BOUNDS, Ev.PRE_RENDER));
        }

        // Documented as "only called if" bounds are active; the converse isn't promised.
        check("POST_COMPOSITE only with viewport bounds (bounds=" + capturedHadBounds + ")",
                capturedHadBounds || !order.contains(Ev.POST_COMPOSITE));
    }

    private static boolean before(List<Ev> order, Ev a, Ev b) {
        int ia = order.indexOf(a);
        int ib = order.indexOf(b);
        return ia >= 0 && ib >= 0 && ia < ib;
    }

    private static boolean sameBounds(@Nullable ViewportBounds a, @Nullable ViewportBounds b) {
        return a == null ? b == null : a.equals(b);
    }

    private static void check(String label, boolean pass) {
        check(label, Boolean.valueOf(pass));
    }

    /**
     * @param pass <code>null</code> for "not yet observed"
     */
    private static void check(String label, @Nullable Boolean pass) {
        if (pass == null) {
            ImGui.textColored(0.7f, 0.7f, 0.7f, 1, "[ -- ] " + label);
        } else if (pass) {
            ImGui.textColored(0.3f, 1f, 0.3f, 1, "[PASS] " + label);
        } else {
            ImGui.textColored(1f, 0.3f, 0.3f, 1, "[FAIL] " + label);
        }
    }
}
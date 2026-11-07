package com.igrium.craftui.api.controls;

import com.igrium.craftui.api.CraftUI;
import imgui.ImGui;
import imgui.extension.imguizmo.ImGuizmo;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseButton;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * Automatically handles activating viewport controls based on the active MC player
 * <p><b>The local client must have full control over player positioning!</b> This usually means spectator mode.</p>
 */
public final class ViewportControlHelper {
    @Getter
    private @Nullable ViewportControls.State state;

    @Getter
    private @NotNull ViewportControls.ControlMode controlMode = ViewportControls.ControlMode.ORBIT;

    @Getter @Setter
    private int orbitButton = ImGuiMouseButton.Left;

    @Getter @Setter
    private int orbitMods = ImGuiKey.ImGuiMod_Alt;

    @Getter @Setter
    private int panButton = ImGuiMouseButton.Middle;

    @Getter @Setter
    private int panMods = ImGuiKey.ImGuiMod_Alt;

    @Getter @Setter
    private int zoomButton = ImGuiMouseButton.Middle;

    @Getter @Setter
    private int zoomMods = 0;

    @Getter @Setter
    private double fallbackOrbitDistance = 10;

    /**
     * @see ViewportControls.Flags
     */
    @Getter @Setter
    private int flags = 0;

    public boolean isMoving() {
        return state != null;
    }


    /**
     * Apply a discrete, single-frame move.
     *
     * @param controlMode Control mode to use
     * @param dx          Effective mouse X delta
     * @param dy          Effective mouse Y delta
     */
    public void applyDiscreteMove(ViewportControls.ControlMode controlMode, float dx, float dy) {
        MouseHandler mouse = Minecraft.getInstance().mouseHandler;

        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        Vector3d pos = new Vector3d();
        Quaterniond rot = new Quaterniond();
        getCameraPos(player, pos, rot);

        Vector3d center = doRaycast(pos, rot, (float) mouse.xpos(), (float) mouse.ypos(), player);

        var state = new ViewportControls.State(center, pos, rot);
        ViewportControls.doViewportMove(controlMode, state, dx, dy, flags);
        setCameraPos(player, state.evalPos(), state.evalRot());
    }

    /**
     * To be called during viewport draw
     */
    public void handleViewportControls() {
        Player player = Minecraft.getInstance().player;

        if (state != null) {
            if (ImGui.isAnyMouseDown() && player != null) {
                // Continue move
                ViewportControls.doViewportMove(controlMode, state,
                        ImGui.getIO().getMouseDeltaX(), ImGui.getIO().getMouseDeltaY(), flags);
                applyViewportMove(state, player);
            } else {
                // End move
                state = null;
            }
        } else if (ImGui.isWindowHovered() && player != null && !ImGuizmo.isOver() && !ImGuizmo.isUsing()) {
            int mods = ImGui.getIO().getKeyMods();

            // Try begin move
            if (ImGui.isMouseDragging(orbitButton) && mods == orbitMods) {
                beginViewportMove(ViewportControls.ControlMode.ORBIT, player);
            } else if (ImGui.isMouseDragging(panButton) && mods == panMods) {
                beginViewportMove(ViewportControls.ControlMode.PAN, player);
            } else if (ImGui.isMouseDragging(zoomButton) && mods == zoomMods) {
                beginViewportMove(ViewportControls.ControlMode.ZOOM, player);
            }
        }
    }

    private static void getCameraPos(Entity ent, Vector3d outPos, Quaterniond outRot) {
        Vec3 eye = ent.getEyePosition();
        outPos.set(eye.x, eye.y, eye.z);

        // Native camera does this same math. Why does Minecraft's rotation handling have to be so stupid?
        outRot.identity().rotationYXZ(
                Math.PI - Math.toRadians(ent.getYRot()),
                -Math.toRadians(ent.getXRot()),
                0
        );
    }

    private void beginViewportMove(ViewportControls.ControlMode controlMode, Player player) {
        Vector3d pos = new Vector3d();
        Quaterniond rot = new Quaterniond();
        getCameraPos(player, pos, rot);

        MouseHandler mouse = Minecraft.getInstance().mouseHandler;
        Vector3d center = doRaycast(pos, rot, (float) mouse.xpos(), (float) mouse.ypos(), player);

        this.state = new ViewportControls.State(center, pos, rot);
        this.controlMode = controlMode;
    }

    private Vector3d doRaycast(Vector3dc pos, Quaterniondc rot, float mouseX, float mouseY, Player except) {
        HitResult raycast = CraftUI.raycastViewport(mouseX, mouseY, 1000, e -> e != except, false);
        if (raycast.getType() != HitResult.Type.MISS) {
            Vec3 hitPos = raycast.getLocation();
            return new Vector3d(hitPos.x, hitPos.y, hitPos.z);
        } else {
            return rot.transform(new Vector3d(0, 0, -1)).mul(fallbackOrbitDistance).add(pos);
        }
    }

//    private HitResult doRaycast(float mouseX, float mouseY, @Nullable Entity except) {
//        return CraftUI.raycastViewport(mouseX, mouseY, 1000, e -> e != except, false);
//    }

    private static void setCameraPos(Entity ent, Vector3dc pos, Quaterniondc rot) {
        Vector3d forward = rot.transform(new Vector3d(0, 0, -1));

        // Inverse of Entity#calculateViewVector: forward = (-sin(yaw)cos(pitch), -sin(pitch), cos(yaw)cos(pitch))
        float yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        float pitch = (float) Math.toDegrees(Math.asin(Math.clamp(-forward.y, -1, 1)));

        double feetY = pos.y() - ent.getEyeHeight();
        ent.setPos(pos.x(), feetY, pos.z());

        ent.setYRot(yaw);
        ent.setYHeadRot(yaw);
        ent.setXRot(pitch);
    }

    private void applyViewportMove(ViewportControls.State state, Player player) {
        setCameraPos(player, state.evalPos(), state.evalRot());
    }
}

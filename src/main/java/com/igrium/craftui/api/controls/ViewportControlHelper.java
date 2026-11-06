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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;

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

    private void beginViewportMove(ViewportControls.ControlMode controlMode, Player player) {
        Vec3 eye = player.getEyePosition();
        Vector3d pos = new Vector3d(eye.x, eye.y, eye.z);

        // Camera does this same math. Why does Minecraft's rotation handling have to be so stupid?
        Quaterniond rot = new Quaterniond().rotationYXZ(
                Math.PI - Math.toRadians(player.getYRot()),
                -Math.toRadians(player.getXRot()),
                0
        );

        MouseHandler mouse = Minecraft.getInstance().mouseHandler;
        HitResult raycast = CraftUI.raycastViewport((float) mouse.xpos(), (float) mouse.ypos(), 1000,
                e -> e != player, false);

        Vector3d center;
        if (raycast.getType() != HitResult.Type.MISS) {
            Vec3 hitPos = raycast.getLocation();
            center = new Vector3d(hitPos.x, hitPos.y, hitPos.z);
        } else {
            center = rot.transform(new Vector3d(0, 0, -0)).mul(fallbackOrbitDistance).add(pos);
        }

        this.state = new ViewportControls.State(center, pos, rot);
        this.controlMode = controlMode;
    }

    private void applyViewportMove(ViewportControls.State state, Player player) {
        Vector3d camPos = state.evalPos();
        Vector3d forward = state.evalRot().transform(new Vector3d(0, 0, -1));

        // Inverse of Entity#calculateViewVector: forward = (-sin(yaw)cos(pitch), -sin(pitch), cos(yaw)cos(pitch))
        float yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        float pitch = (float) Math.toDegrees(Math.asin(Math.clamp(-forward.y, -1, 1)));

        double feetY = camPos.y - player.getEyeHeight();
        player.setPos(camPos.x, feetY, camPos.z);

        player.setYRot(yaw);
        player.setYHeadRot(yaw);
        player.setXRot(pitch);
    }
}

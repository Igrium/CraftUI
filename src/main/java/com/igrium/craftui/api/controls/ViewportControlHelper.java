package com.igrium.craftui.api.controls;

import com.igrium.craftui.api.CraftUI;
import com.igrium.craftui.api.controls.ViewportControls.ControlMode;
import imgui.ImGui;
import imgui.extension.imguizmo.ImGuizmo;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseButton;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.world.entity.Entity;
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
 * <p>Can be used as-is, or subclassed to customize behavior.</p>
 */
@Accessors(chain = true)
public class ViewportControlHelper {
    @Getter
    @Accessors(makeFinal = true)
    private @Nullable ViewportControls.State state;

    @Getter
    @Accessors(makeFinal = true)
    private @NotNull ControlMode controlMode = ControlMode.ORBIT;

    @Getter
    @Setter
    private int orbitButton = ImGuiMouseButton.Left;

    @Getter
    @Setter
    private int orbitMods = ImGuiKey.ImGuiMod_Alt;

    @Getter
    @Setter
    private int panButton = ImGuiMouseButton.Middle;

    @Getter
    @Setter
    private int panMods = ImGuiKey.ImGuiMod_Alt;

    @Getter
    @Setter
    private int zoomButton = ImGuiMouseButton.Middle;

    @Getter
    @Setter
    private int zoomMods = 0;

    @Getter
    @Setter
    private double fallbackOrbitDistance = 10;

    @Getter
    @Setter
    private float maxRaycastDistance = 1000;

    /**
     * @see ViewportControls.Flags
     */
    @Getter
    @Setter
    private int flags = 0;

    public final boolean isMoving() {
        return state != null;
    }

    /**
     * End the current viewport move, if any.
     */
    public void endMove() {
        state = null;
    }

    /**
     * Apply a discrete, single-frame move.
     *
     * @param controlMode Control mode to use
     * @param dx          Effective mouse X delta
     * @param dy          Effective mouse Y delta
     */
    public void applyDiscreteMove(ControlMode controlMode, float dx, float dy) {
        Entity camera = getCameraEntity();
        if (camera == null) return;

        var state = createState(camera);
        performMove(controlMode, state, dx, dy);
        setCameraTransform(camera, state.evalPos(), state.evalRot());
    }

    /**
     * To be called during viewport draw
     */
    public void handleViewportControls() {
        Entity camera = getCameraEntity();

        if (getState() != null) {
            if (camera != null && shouldContinueMove()) {
                // Continue move
                performMove(getControlMode(), getState(), ImGui.getIO().getMouseDeltaX(),
                        ImGui.getIO().getMouseDeltaY());
                setCameraTransform(camera, getState().evalPos(), getState().evalRot());
            } else {
                endMove();
            }
        } else if (camera != null && canBeginMove()) {
            ControlMode mode = pollBeginMove();
            if (mode != null) {
                beginMove(mode, camera);
            }
        }

        float scrollY = getMouseWheel();
        if (Math.abs(scrollY) > .01f) {
            applyDiscreteMove(ControlMode.ZOOM, 0, scrollY);
        }
    }

    /**
     * Return the amount that the viewport should zoom due to the mouse wheel on this frame
     *
     * @apiNote Called every frame, even if the viewport is not hovered.
     */
    protected float getMouseWheel() {
        // I eyeballed the -12
        return ImGui.isWindowHovered() ? ImGui.getIO().getMouseWheel() * -12 : 0;
    }

    /**
     * Get the entity that the viewport controls should move.
     *
     * @return The camera entity, or <code>null</code> if there is none.
     */
    protected @Nullable Entity getCameraEntity() {
        return Minecraft.getInstance().player;
    }

    /**
     * Check whether a new viewport move is allowed to begin this frame.
     */
    protected boolean canBeginMove() {
        return ImGui.isWindowHovered() && !ImGuizmo.isOver() && !ImGuizmo.isUsing();
    }

    /**
     * Determine which control mode, if any, the user is currently trying to begin.
     *
     * @return The control mode to begin, or <code>null</code> to not begin a move.
     */
    protected @Nullable ControlMode pollBeginMove() {
        int mods = ImGui.getIO().getKeyMods();

        if (ImGui.isMouseDragging(getOrbitButton()) && mods == getOrbitMods()) {
            return ControlMode.ORBIT;
        } else if (ImGui.isMouseDragging(getPanButton()) && mods == getPanMods()) {
            return ControlMode.PAN;
        } else if (ImGui.isMouseDragging(getZoomButton()) && mods == getZoomMods()) {
            return ControlMode.ZOOM;
        }
        return null;
    }

    /**
     * Check whether the current viewport move should continue this frame.
     */
    protected boolean shouldContinueMove() {
        return ImGui.isAnyMouseDown();
    }

    /**
     * Apply mouse movement to a viewport state.
     *
     * @param controlMode Control mode to use
     * @param state       State to update
     * @param dx          Mouse X delta
     * @param dy          Mouse Y delta
     */
    protected void performMove(ControlMode controlMode, ViewportControls.State state, float dx,
                               float dy) {
        ViewportControls.doViewportMove(controlMode, state, dx, dy, getFlags());
    }

    /**
     * Read the current transform of the camera entity.
     *
     * @param ent    Camera entity
     * @param outPos Destination for the camera position
     * @param outRot Destination for the camera rotation
     */
    protected void getCameraTransform(Entity ent, Vector3d outPos, Quaterniond outRot) {
        Vec3 eye = ent.getEyePosition();
        outPos.set(eye.x, eye.y, eye.z);

        // Native camera does this same math. Why does Minecraft's rotation handling have to be so stupid?
        outRot.identity().rotationYXZ(
                Math.PI - Math.toRadians(ent.getYRot()),
                -Math.toRadians(ent.getXRot()),
                0
        );
    }

    /**
     * Begin a continuous viewport move.
     *
     * @param controlMode Control mode to use
     * @param camera      Camera entity
     */
    protected void beginMove(ControlMode controlMode, Entity camera) {
        this.state = createState(camera);
        this.controlMode = controlMode;
    }

    /**
     * Create a viewport state from the camera's current transform.
     *
     * @param camera Camera entity
     * @return The new state
     */
    protected ViewportControls.State createState(Entity camera) {
        Vector3d pos = new Vector3d();
        Quaterniond rot = new Quaterniond();
        getCameraTransform(camera, pos, rot);

        Vector3d center = computeOrbitCenter(pos, rot, camera);
        return new ViewportControls.State(center, pos, rot);
    }

    /**
     * Compute the point that orbit/zoom moves should center around.
     * Defaults to a raycast under the mouse, falling back to a point {@link #getFallbackOrbitDistance()} in front of
     * the camera.
     *
     * @param pos    Camera position
     * @param rot    Camera rotation
     * @param camera Camera entity
     * @return The orbit center
     */
    protected Vector3d computeOrbitCenter(Vector3dc pos, Quaterniondc rot, Entity camera) {
        MouseHandler mouse = Minecraft.getInstance().mouseHandler;
        HitResult raycast = CraftUI.raycastViewport((float) mouse.xpos(), (float) mouse.ypos(),
                getMaxRaycastDistance(), e -> e != camera, false);
        if (raycast.getType() != HitResult.Type.MISS) {
            Vec3 hitPos = raycast.getLocation();
            return new Vector3d(hitPos.x, hitPos.y, hitPos.z);
        } else {
            return rot.transform(new Vector3d(0, 0, -1)).mul(getFallbackOrbitDistance()).add(pos);
        }
    }

    /**
     * Apply a transform to the camera entity.
     *
     * @param ent Camera entity
     * @param pos New camera (eye) position
     * @param rot New camera rotation
     */
    protected void setCameraTransform(Entity ent, Vector3dc pos, Quaterniondc rot) {
        Vector3d forward = rot.transform(new Vector3d(0, 0, -1));

        // Inverse of Entity#calculateViewVector: forward = (-sin(yaw)cos(pitch), -sin(pitch), cos(yaw)cos(pitch))
        float yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        float pitch = (float) Math.toDegrees(Math.asin(Math.clamp(-forward.y, -1, 1)));

        double feetY = pos.y() - ent.getEyeHeight();
        ent.setPos(pos.x(), feetY, pos.z());

        ent.setYRot(yaw);
        ent.setYHeadRot(yaw);
        ent.setXRot(pitch);
        ent.setOldPosAndRot();
    }
}

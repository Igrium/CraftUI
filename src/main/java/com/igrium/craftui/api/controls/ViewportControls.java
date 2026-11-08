package com.igrium.craftui.api.controls;

import lombok.experimental.UtilityClass;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3dc;

/**
 * A utility class to 3d-editor-like orbit/pan/zoom controls
 * <p>
 * Conventions used by all viewport operations:
 * <ul>
 *     <li>Camera space follows Minecraft / OpenGL: +X right, +Y up, -Z forward.</li>
 *     <li>Mouse deltas are <b>per-frame</b> movement in screen pixels (+X right, +Y down).
 *     They are accumulated into {@link State#mouseOffset()}, and
 *     {@link State#deltaPos()}/{@link State#deltaRot()} are recomputed absolutely
 *     from the start state and that total, so there is no drift from accumulated floating-point error.</li>
 *     <li>A {@link State} represents a single drag in a single mode. Switching modes on
 *     the same state would reinterpret the whole accumulated offset in the new mode.</li>
 * </ul>
 *
 * <blockquote>
 * So yeah I vibe-coded this. I built the structure, and then decided I didn't wanna spend two hours
 * figuring out the math lol.
 * </blockquote>
 */
@UtilityClass
public class ViewportControls {

    @UtilityClass
    public final class Flags {
        public static final int ALLOW_ROLL = 1;
    }

    public enum ControlMode {
        ORBIT, PAN, ZOOM
    }

    /**
     * Represents a persistent "state" of a viewport move
     *
     * @param centerPos   Center position of the orbit.
     * @param startPos    Initial camera position
     * @param startRot    Initial camera rotation
     * @param deltaPos    Delta camera position
     * @param deltaRot    Delta camera rotation
     * @param mouseOffset Accumulated mouse movement (in pixels) since the drag started
     */
    public record State(Vector3dc centerPos,
                        Vector3dc startPos, Quaterniondc startRot,
                        Vector3d deltaPos, Quaterniond deltaRot,
                        Vector2d mouseOffset) {

        public State(Vector3dc centerPos,
                     Vector3dc startPos, Quaterniondc startRot,
                     Vector3d deltaPos, Quaterniond deltaRot,
                     Vector2d mouseOffset) {

            this.centerPos = new Vector3d(centerPos);
            this.startPos = new Vector3d(startPos);
            this.startRot = new Quaterniond(startRot);
            this.deltaPos = deltaPos;
            this.deltaRot = deltaRot;
            this.mouseOffset = mouseOffset;
        }

        public State(Vector3dc centerPos, Vector3dc startPos, Quaterniondc startRot) {
            this(centerPos, startPos, startRot, new Vector3d(), new Quaterniond(), new Vector2d());
        }

        /**
         * Resolve the current camera position (start + delta)
         *
         * @param dest Will hold the result
         * @return <code>dest</code>
         */
        public Vector3d evalPos(Vector3d dest) {
            return startPos.add(deltaPos, dest);
        }

        /**
         * Resolve the current camera position (start + delta)
         *
         * @return Resolved position
         */
        public Vector3d evalPos() {
            return evalPos(new Vector3d());
        }

        /**
         * Resolve the current camera rotation (delta * start)
         *
         * @param dest Will hold the result
         * @return <code>dest</code>
         */
        public Quaterniond evalRot(Quaterniond dest) {
            return startRot.premul(deltaRot, dest);
        }

        /**
         * Resolve the current camera rotation (delta * start)
         *
         * @return Resolved rotation
         */
        public Quaterniond evalRot() {
            return evalRot(new Quaterniond());
        }

        /**
         * Resolve the current camera rotation (delta * start) at single precision
         *
         * @param dest Will hold the result
         * @return <code>dest</code>
         */
        public Quaternionf evalRot(Quaternionf dest) {
            return dest.set(evalRot());
        }
    }

    /**
     * Orbit rotation speed, in radians per pixel.
     */
    private static final double ORBIT_SENSITIVITY = 0.008;

    /**
     * Pan speed, in world units per pixel <i>per unit of distance</i> from the orbit center.
     * The default roughly matches a 70 degree vertical FOV on a 1080p viewport, so geometry
     * at the center's depth stays approximately under the cursor.
     */
    private static final double PAN_SENSITIVITY = 0.0013;

    /**
     * Zoom speed. Orbit distance is scaled by <code>e^(pixels * ZOOM_SENSITIVITY)</code>,
     * so zooming is exponential: always proportional to the current distance, and never
     * passes through the center.
     */
    private static final double ZOOM_SENSITIVITY = 0.01;

    /**
     * Minimum distance used when scaling pan speed, so panning still works when the
     * camera is (nearly) on top of the orbit center.
     */
    private static final double MIN_PAN_DISTANCE = 0.1;

    private static final double EPSILON = 1e-8;

    /**
     * Perform a viewport move
     *
     * @param mode        Control mode to use
     * @param state       Current move state
     * @param mouseDeltaX Delta X this frame
     * @param mouseDeltaY Delta Y this frame
     * @param flags       {@link Flags}
     */
    public static void doViewportMove(ControlMode mode, State state,
                                      float mouseDeltaX, float mouseDeltaY, int flags) {
        switch (mode) {
            case ORBIT -> doViewportOrbit(state, mouseDeltaX, mouseDeltaY, flags);
            case PAN -> doViewportPan(state, mouseDeltaX, mouseDeltaY, flags);
            case ZOOM -> doViewportZoom(state, mouseDeltaX, mouseDeltaY, flags);
        }
    }

    /**
     * Orbit the camera around <code>centerPos</code>.
     *
     * @param state       Current move state
     * @param mouseDeltaX Delta X this frame
     * @param mouseDeltaY Delta Y this frame
     * @param flags       {@link Flags}
     */
    public static void doViewportOrbit(State state, float mouseDeltaX, float mouseDeltaY, int flags) {
        Vector2d total = state.mouseOffset().add(mouseDeltaX, mouseDeltaY);
        double yaw = -total.x * ORBIT_SENSITIVITY;
        double pitch = -total.y * ORBIT_SENSITIVITY;

        Quaterniondc startRot = state.startRot();

        // Pitch around the camera's right axis (in world space)
        Vector3d right = startRot.transform(new Vector3d(1, 0, 0)).normalize();
        Quaterniond pitchRot = new Quaterniond().rotationAxis(pitch, right.x, right.y, right.z);

        // Yaw around world up (turntable) or the camera's up (trackball)
        Quaterniond yawRot;
        if ((flags & Flags.ALLOW_ROLL) != 0) {
            Vector3d up = startRot.transform(new Vector3d(0, 1, 0)).normalize();
            yawRot = new Quaterniond().rotationAxis(yaw, up.x, up.y, up.z);
        } else {
            yawRot = new Quaterniond().rotationY(yaw);
        }

        Quaterniond deltaRot = yawRot.mul(pitchRot).normalize();

        // Rotate the camera's offset from the center by the same rotation
        Vector3d offset = state.startPos().sub(state.centerPos(), new Vector3d());
        Vector3d newOffset = deltaRot.transform(new Vector3d(offset));

        state.deltaRot().set(deltaRot);
        state.deltaPos().set(newOffset).sub(offset);
    }

    /**
     * <p>Pan the camera parallel to the view plane</p>
     * <p>Dragging moves the scene with the mouse. Speed scales linearly with distance to <code>centerPos</code></p>
     *
     * @param state       Current move state
     * @param mouseDeltaX Delta X this frame
     * @param mouseDeltaY Delta Y this frame
     * @param flags       {@link Flags}
     */
    public static void doViewportPan(State state, float mouseDeltaX, float mouseDeltaY, int flags) {
        Vector2d total = state.mouseOffset().add(mouseDeltaX, mouseDeltaY);
        double distance = Math.max(state.startPos().distance(state.centerPos()), MIN_PAN_DISTANCE);
        double scale = distance * PAN_SENSITIVITY;

        Quaterniondc startRot = state.startRot();
        Vector3d right = startRot.transform(new Vector3d(1, 0, 0));
        Vector3d up = startRot.transform(new Vector3d(0, 1, 0));

        // Camera moves opposite the drag so the scene follows the cursor (screen Y is down).
        state.deltaPos().set(right.mul(-total.x * scale)).add(up.mul(total.y * scale));
        state.deltaRot().identity();
    }

    /**
     * <p>Dolly the camera towards/away from <code>centerPos</code></p>
     * <p>Dragging up zooms in; dragging down zooms out. Zoom is exponential, so it's speed is proportional to the
     * current distance from the center.</p>
     *
     * @param state       Current move state
     * @param mouseDeltaX Delta X this frame
     * @param mouseDeltaY Delta Y this frame
     * @param flags       {@link Flags}
     */
    public static void doViewportZoom(State state, float mouseDeltaX, float mouseDeltaY, int flags) {
        Vector2d total = state.mouseOffset().add(mouseDeltaX, mouseDeltaY);
        Vector3d offset = state.startPos().sub(state.centerPos(), new Vector3d());
        state.deltaRot().identity();

        if (offset.lengthSquared() < EPSILON) {
            state.deltaPos().zero();
            return;
        }

        double factor = Math.exp(total.y * ZOOM_SENSITIVITY);
        // newPos = center + offset * factor  =>  deltaPos = offset * (factor - 1)
        state.deltaPos().set(offset).mul(factor - 1);
    }
}

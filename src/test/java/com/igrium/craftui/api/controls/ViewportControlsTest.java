package com.igrium.craftui.api.controls;

import com.igrium.craftui.api.controls.ViewportControls.ControlMode;
import com.igrium.craftui.api.controls.ViewportControls.State;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Specification tests for {@link ViewportControls}. These describe how 3D-editor viewport controls
 * are expected to behave (Blender / Maya style), independent of the exact sensitivity values.
 */
class ViewportControlsTest {

    private static final double EPS = 1e-6;
    private static final Vector3dc WORLD_UP = new Vector3d(0, 1, 0);

    // ---------------------------------------------------------------- helpers

    /** Camera rotation whose forward (-Z) points along <code>dir</code>, with no roll. */
    private static Quaterniond lookRot(Vector3dc dir) {
        // Build the camera basis explicitly: columns are camera +X (right), +Y (up), +Z (back) in world space.
        Vector3d f = new Vector3d(dir).normalize();
        Vector3d r = f.cross(WORLD_UP, new Vector3d()).normalize();
        Vector3d u = r.cross(f, new Vector3d());
        Quaterniond rot = new Quaterniond().setFromNormalized(new Matrix3d(r, u, f.negate(new Vector3d())));
        assertVecEquals(new Vector3d(dir).normalize(), forward(rot), "lookRot helper is broken");
        assertTrue(up(rot).y > 0, "lookRot helper produced an upside-down camera");
        assertEquals(0.0, right(rot).y, EPS, "lookRot helper produced a rolled camera");
        return rot;
    }

    /** A state with the camera at <code>pos</code> looking directly at <code>center</code>. */
    private static State lookingAt(Vector3dc center, Vector3dc pos) {
        return new State(center, pos, lookRot(center.sub(pos, new Vector3d())));
    }

    /** A camera 10 units back and 5 up from a non-origin center, looking at it. */
    private static State defaultState() {
        Vector3d center = new Vector3d(3, 64, -7);
        return lookingAt(center, center.add(0, 5, 10, new Vector3d()));
    }

    private static Vector3d forward(Quaterniondc rot) {
        return rot.transform(new Vector3d(0, 0, -1));
    }

    private static Vector3d right(Quaterniondc rot) {
        return rot.transform(new Vector3d(1, 0, 0));
    }

    private static Vector3d up(Quaterniondc rot) {
        return rot.transform(new Vector3d(0, 1, 0));
    }

    /** Unsigned angle between the start and current heading, projected onto the horizontal plane. */
    private static double headingAngle(State state) {
        Vector3d start = forward(state.startRot()).mul(1, 0, 1).normalize();
        Vector3d current = forward(state.evalRot()).mul(1, 0, 1).normalize();
        return start.angle(current);
    }

    private static double distToCenter(State state) {
        return state.evalPos().distance(state.centerPos());
    }

    private static Vector3d displacement(State state) {
        return state.evalPos().sub(state.startPos());
    }

    private static void assertVecEquals(Vector3dc expected, Vector3dc actual, String message) {
        assertTrue(expected.distance(actual) < EPS,
                () -> message + ": expected " + expected + " but was " + actual);
    }

    private static void assertVecEquals(Vector3dc expected, Vector3dc actual) {
        assertVecEquals(expected, actual, "vectors differ");
    }

    private static void assertRotEquals(Quaterniondc expected, Quaterniondc actual) {
        // q and -q represent the same rotation, so compare by their effect on the basis vectors.
        assertVecEquals(forward(expected), forward(actual), "forward differs");
        assertVecEquals(up(expected), up(actual), "up differs");
    }

    // ---------------------------------------------------------------- general

    @ParameterizedTest
    @EnumSource(ControlMode.class)
    void noMovementLeavesCameraUnchanged(ControlMode mode) {
        State state = defaultState();
        ViewportControls.doViewportMove(mode, state, 0, 0, 0);

        assertVecEquals(state.startPos(), state.evalPos());
        assertRotEquals(state.startRot(), state.evalRot());
    }

    @ParameterizedTest
    @EnumSource(ControlMode.class)
    void perFrameDeltasAccumulateToSameResultAsOneMove(ControlMode mode) {
        State incremental = defaultState();
        for (int i = 0; i < 40; i++) {
            ViewportControls.doViewportMove(mode, incremental, 3f, -2f, 0);
        }

        State single = defaultState();
        ViewportControls.doViewportMove(mode, single, 120f, -80f, 0);

        assertVecEquals(single.evalPos(), incremental.evalPos());
        assertRotEquals(single.evalRot(), incremental.evalRot());
    }

    @ParameterizedTest
    @EnumSource(ControlMode.class)
    void movingBackToStartRestoresCamera(ControlMode mode) {
        State state = defaultState();
        ViewportControls.doViewportMove(mode, state, 57f, -31f, 0);
        ViewportControls.doViewportMove(mode, state, -20f, 90f, 0);
        ViewportControls.doViewportMove(mode, state, -37f, -59f, 0);

        assertVecEquals(state.startPos(), state.evalPos());
        assertRotEquals(state.startRot(), state.evalRot());
    }

    @ParameterizedTest
    @EnumSource(ControlMode.class)
    void resultingRotationIsNormalized(ControlMode mode) {
        State state = defaultState();
        ViewportControls.doViewportMove(mode, state, 123f, -456f, 0);
        assertEquals(1.0, state.evalRot().lengthSquared(), EPS);
    }

    @Test
    void startValuesAreNotAffectedByMutatingInputs() {
        Vector3d center = new Vector3d(0, 0, 0);
        Vector3d pos = new Vector3d(0, 0, 5);
        Quaterniond rot = lookRot(new Vector3d(0, 0, -1));
        State state = new State(center, pos, rot);

        center.set(100, 100, 100);
        pos.set(100, 100, 100);
        rot.rotateY(1);

        assertVecEquals(new Vector3d(0, 0, 0), state.centerPos());
        assertVecEquals(new Vector3d(0, 0, 5), state.startPos());
        assertRotEquals(lookRot(new Vector3d(0, 0, -1)), state.startRot());
    }

    // ---------------------------------------------------------------- orbit

    @Nested
    class Orbit {

        @Test
        void preservesDistanceToCenter() {
            State state = defaultState();
            double startDist = distToCenter(state);
            ViewportControls.doViewportOrbit(state, 150f, -70f, 0);
            assertEquals(startDist, distToCenter(state), EPS);
        }

        @Test
        void keepsLookingAtCenter() {
            State state = defaultState();
            ViewportControls.doViewportOrbit(state, -90f, 45f, 0);

            Vector3d toCenter = state.centerPos().sub(state.evalPos(), new Vector3d()).normalize();
            assertVecEquals(toCenter, forward(state.evalRot()));
        }

        @Test
        void actuallyMovesCamera() {
            State state = defaultState();
            ViewportControls.doViewportOrbit(state, 50f, 50f, 0);
            assertTrue(displacement(state).length() > 1e-3);
        }

        @Test
        void dragRightOrbitsCameraToItsLeft() {
            // The scene follows the mouse: dragging right spins the scene right, so the camera moves left.
            State state = defaultState();
            ViewportControls.doViewportOrbit(state, 20f, 0, 0);
            assertTrue(displacement(state).dot(right(state.startRot())) < 0);
        }

        @Test
        void dragUpOrbitsCameraDownward() {
            // The scene follows the mouse: dragging up (negative screen Y) tilts the scene up,
            // so the camera moves down to look from below.
            State state = defaultState();
            ViewportControls.doViewportOrbit(state, 0, -20f, 0);
            assertTrue(displacement(state).dot(up(state.startRot())) < 0);
        }

        @Test
        void turntableNeverRolls() {
            State state = defaultState();
            ViewportControls.doViewportOrbit(state, 170f, -130f, 0);
            // With no roll, the camera's right vector stays horizontal.
            assertEquals(0.0, right(state.evalRot()).y, EPS);
        }

        @Test
        void turntableHorizontalDragRotatesAroundWorldUp() {
            State state = defaultState();
            ViewportControls.doViewportOrbit(state, 200f, 0, 0);

            // Height relative to the center doesn't change, and neither does the pitch.
            assertEquals(state.startPos().y(), state.evalPos().y, EPS);
            assertEquals(forward(state.startRot()).y, forward(state.evalRot()).y, EPS);
        }

        @Test
        void turntableVerticalDragDoesNotChangeHeading() {
            State state = defaultState();
            ViewportControls.doViewportOrbit(state, 0, 60f, 0);

            Vector3d startHeading = forward(state.startRot()).mul(1, 0, 1).normalize();
            Vector3d heading = forward(state.evalRot()).mul(1, 0, 1).normalize();
            assertVecEquals(startHeading, heading);
        }

        @Test
        void allowRollHorizontalDragRotatesAroundCameraUp() {
            State state = defaultState(); // pitched down, so camera up != world up
            ViewportControls.doViewportOrbit(state, 200f, 0, ViewportControls.Flags.ALLOW_ROLL);

            assertVecEquals(up(state.startRot()), up(state.evalRot()), "camera up should be the rotation axis");
            assertEquals(distToCenter(new State(state.centerPos(), state.startPos(), state.startRot())),
                    distToCenter(state), EPS);
        }

        @Test
        void allowRollKeepsLookingAtCenter() {
            State state = defaultState();
            ViewportControls.doViewportOrbit(state, 80f, -120f, ViewportControls.Flags.ALLOW_ROLL);

            Vector3d toCenter = state.centerPos().sub(state.evalPos(), new Vector3d()).normalize();
            assertVecEquals(toCenter, forward(state.evalRot()));
        }

        @Test
        void largerDragOrbitsFurther() {
            State small = defaultState();
            State large = defaultState();
            ViewportControls.doViewportOrbit(small, 10f, 0, 0);
            ViewportControls.doViewportOrbit(large, 40f, 0, 0);

            // Orbit angle should be linear in mouse movement. Measure the yaw (heading in the horizontal
            // plane) rather than the 3D angle between forward vectors, which isn't linear for a pitched camera.
            double smallAngle = headingAngle(small);
            double largeAngle = headingAngle(large);
            assertTrue(smallAngle > 0);
            assertEquals(4 * smallAngle, largeAngle, 1e-9);
        }
    }

    // ---------------------------------------------------------------- pan

    @Nested
    class Pan {

        @Test
        void doesNotRotate() {
            State state = defaultState();
            ViewportControls.doViewportPan(state, 80f, -45f, 0);
            assertRotEquals(state.startRot(), state.evalRot());
        }

        @Test
        void movesParallelToViewPlane() {
            State state = defaultState();
            ViewportControls.doViewportPan(state, 80f, -45f, 0);

            Vector3d disp = displacement(state);
            assertTrue(disp.length() > 1e-3);
            assertEquals(0.0, disp.dot(forward(state.startRot())), EPS);
        }

        @Test
        void dragRightMovesCameraLeft() {
            // The scene follows the mouse, so the camera moves opposite to the drag.
            State state = defaultState();
            ViewportControls.doViewportPan(state, 30f, 0, 0);

            Vector3d disp = displacement(state);
            assertTrue(disp.dot(right(state.startRot())) < 0);
            assertEquals(0.0, disp.dot(up(state.startRot())), EPS);
        }

        @Test
        void dragDownMovesCameraUp() {
            // Screen Y is down; the scene follows the mouse down, so the camera moves up.
            State state = defaultState();
            ViewportControls.doViewportPan(state, 0, 30f, 0);

            Vector3d disp = displacement(state);
            assertTrue(disp.dot(up(state.startRot())) > 0);
            assertEquals(0.0, disp.dot(right(state.startRot())), EPS);
        }

        @Test
        void isLinearInMouseMovement() {
            State once = defaultState();
            State twice = defaultState();
            ViewportControls.doViewportPan(once, 25f, 10f, 0);
            ViewportControls.doViewportPan(twice, 50f, 20f, 0);

            assertVecEquals(displacement(once).mul(2), displacement(twice));
        }

        @Test
        void speedScalesWithDistanceToCenter() {
            Vector3d center = new Vector3d(0, 0, 0);
            State near = lookingAt(center, new Vector3d(0, 0, 5));
            State far = lookingAt(center, new Vector3d(0, 0, 20));

            ViewportControls.doViewportPan(near, 40f, -15f, 0);
            ViewportControls.doViewportPan(far, 40f, -15f, 0);

            assertEquals(4 * displacement(near).length(), displacement(far).length(), EPS);
            assertVecEquals(displacement(near).normalize(), displacement(far).normalize());
        }

        @Test
        void stillMovesWhenCameraIsAtCenter() {
            Vector3d center = new Vector3d(1, 2, 3);
            State state = new State(center, center, lookRot(new Vector3d(0, 0, -1)));
            ViewportControls.doViewportPan(state, 40f, 0, 0);

            assertTrue(displacement(state).length() > 1e-6);
        }
    }

    // ---------------------------------------------------------------- zoom

    @Nested
    class Zoom {

        @Test
        void dragUpZoomsIn() {
            State state = defaultState();
            double startDist = distToCenter(state);
            ViewportControls.doViewportZoom(state, 0, -30f, 0);
            assertTrue(distToCenter(state) < startDist);
        }

        @Test
        void dragDownZoomsOut() {
            State state = defaultState();
            double startDist = distToCenter(state);
            ViewportControls.doViewportZoom(state, 0, 30f, 0);
            assertTrue(distToCenter(state) > startDist);
        }

        @Test
        void doesNotRotate() {
            State state = defaultState();
            ViewportControls.doViewportZoom(state, 0, -50f, 0);
            assertRotEquals(state.startRot(), state.evalRot());
        }

        @Test
        void movesAlongLineToCenter() {
            State state = defaultState();
            ViewportControls.doViewportZoom(state, 0, -50f, 0);

            Vector3d startDir = state.startPos().sub(state.centerPos(), new Vector3d()).normalize();
            Vector3d dir = state.evalPos().sub(state.centerPos()).normalize();
            assertVecEquals(startDir, dir);
        }

        @Test
        void neverReachesOrPassesCenter() {
            State state = defaultState();
            ViewportControls.doViewportZoom(state, 0, -100_000f, 0);

            Vector3d startDir = state.startPos().sub(state.centerPos(), new Vector3d());
            Vector3d dir = state.evalPos().sub(state.centerPos());
            // Still on the same side of the center (not flipped through it).
            assertTrue(dir.dot(startDir) >= 0);
            assertTrue(distToCenter(state) <= startDir.length());
        }

        @Test
        void speedIsProportionalToDistance() {
            // The same drag scales the distance by the same factor, regardless of how far away we are.
            Vector3d center = new Vector3d(0, 0, 0);
            State near = lookingAt(center, new Vector3d(0, 0, 2));
            State far = lookingAt(center, new Vector3d(0, 0, 200));

            ViewportControls.doViewportZoom(near, 0, -40f, 0);
            ViewportControls.doViewportZoom(far, 0, -40f, 0);

            assertEquals(distToCenter(near) / 2.0, distToCenter(far) / 200.0, EPS);
        }

        @Test
        void isExponential() {
            // Zooming in by N pixels twice is the same as squaring the zoom factor of N pixels.
            State once = defaultState();
            State twice = defaultState();
            double startDist = distToCenter(once);

            ViewportControls.doViewportZoom(once, 0, -25f, 0);
            ViewportControls.doViewportZoom(twice, 0, -50f, 0);

            double factor = distToCenter(once) / startDist;
            assertEquals(factor * factor, distToCenter(twice) / startDist, EPS);
        }

        @Test
        void zoomInAndOutAreSymmetric() {
            State in = defaultState();
            State out = defaultState();
            double startDist = distToCenter(in);

            ViewportControls.doViewportZoom(in, 0, -35f, 0);
            ViewportControls.doViewportZoom(out, 0, 35f, 0);

            // e^-x * e^x = 1
            assertEquals(1.0, (distToCenter(in) / startDist) * (distToCenter(out) / startDist), EPS);
        }

        @Test
        void cameraAtCenterDoesNotProduceInvalidValues() {
            Vector3d center = new Vector3d(1, 2, 3);
            State state = new State(center, center, lookRot(new Vector3d(0, 0, -1)));
            ViewportControls.doViewportZoom(state, 0, -40f, 0);

            Vector3d pos = state.evalPos();
            assertTrue(pos.isFinite(), () -> "position should be finite, was " + pos);
        }
    }
}
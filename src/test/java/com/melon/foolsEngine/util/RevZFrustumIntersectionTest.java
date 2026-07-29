package com.melon.foolsEngine.util;

import static org.junit.jupiter.api.Assertions.*;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class RevZFrustumIntersectionTest {

    @Test
    void insideFrustumReturnsInside() {
        RevZFrustumIntersection f = new RevZFrustumIntersection();
        Matrix4f vp = new Matrix4f()
                .perspective((float) Math.toRadians(60), 1.6f, 0.01f, 1000f, true)
                .lookAt(0, 0, -5, 0, 0, 0, 0, 1, 0);
        f.setVp(vp);

        int r = f.testAab(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);
        assertTrue(r != RevZFrustumIntersection.OUTSIDE, "AABB in front of camera should not be outside");
    }

    @Test
    void behindCameraReturnsOutside() {
        RevZFrustumIntersection f = new RevZFrustumIntersection();
        Matrix4f vp = new Matrix4f()
                .perspective((float) Math.toRadians(60), 1.6f, 0.01f, 1000f, true)
                .lookAt(0, 0, -5, 0, 0, 0, 0, 1, 0);
        f.setVp(vp);

        int r = f.testAab(9, 9, 9, 10, 10, 10);
        assertEquals(RevZFrustumIntersection.OUTSIDE, r, "AABB behind camera should be outside");
    }

    @Test
    void rotatedFrustumDetectsInside() {
        RevZFrustumIntersection f = new RevZFrustumIntersection();
        Matrix4f vp = new Matrix4f()
                .perspective((float) Math.toRadians(60), 1.6f, 0.01f, 1000f, true)
                .lookAt(5, 2, -5, 0, 0, 0, 0, 1, 0);
        f.setVp(vp);

        int r = f.testAab(-0.1f, -0.1f, -0.1f, 0.1f, 0.1f, 0.1f);
        assertTrue(r != RevZFrustumIntersection.OUTSIDE, "small AABB at origin should be visible from offset camera");
    }

    @Test
    void counterKeepsTrack() {
        RevZFrustumIntersection f = new RevZFrustumIntersection();
        f.resetCounters();
        assertEquals(0, f.testedCount());
        assertEquals(0, f.culledCount());

        Matrix4f vp = new Matrix4f()
                .perspective((float) Math.toRadians(60), 1.6f, 0.01f, 1000f, true)
                .lookAt(0, 0, -5, 0, 0, 0, 0, 1, 0);
        f.setVp(vp);
        f.testAab(-0.1f, -0.1f, -0.1f, 0.1f, 0.1f, 0.1f);
        assertEquals(1, f.testedCount());
    }
}

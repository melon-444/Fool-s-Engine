// foolsEngine - A custom 3D game engine in Java
// Copyright (C) 2026  melon_444
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
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

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

import org.joml.Matrix4fc;

/**
 * Frustum intersection for a reverse-Z, zero-to-one,
 * infinite-far projection.
 *
 * <p>The far plane is intentionally omitted.</p>
 */
public final class RevZFrustumIntersection {

    public static final int OUTSIDE = -1;
    public static final int INTERSECT = 0;
    public static final int INSIDE = 1;

    private static final int PLANE_COUNT = 5;
    private static final float MIN_NORMAL_LENGTH_SQUARED = 1E-20f;

    private final float[] planes = new float[PLANE_COUNT * 4];

    private int culled;
    private int tested;

    public int culledCount() {
        return culled;
    }

    public int testedCount() {
        return tested;
    }

    public void resetCounters() {
        culled = 0;
        tested = 0;
    }

    /**
     * Updates the frustum from projection * view.
     *
     * <p>Assumes OpenGL zero-to-one clip depth and reverse-Z.</p>
     */
    public void setVp(Matrix4fc vp) {
        // Left: clipW + clipX >= 0
        setPlane(
                0,
                vp.m03() + vp.m00(),
                vp.m13() + vp.m10(),
                vp.m23() + vp.m20(),
                vp.m33() + vp.m30()
        );

        // Right: clipW - clipX >= 0
        setPlane(
                1,
                vp.m03() - vp.m00(),
                vp.m13() - vp.m10(),
                vp.m23() - vp.m20(),
                vp.m33() - vp.m30()
        );

        // Bottom: clipW + clipY >= 0
        setPlane(
                2,
                vp.m03() + vp.m01(),
                vp.m13() + vp.m11(),
                vp.m23() + vp.m21(),
                vp.m33() + vp.m31()
        );

        // Top: clipW - clipY >= 0
        setPlane(
                3,
                vp.m03() - vp.m01(),
                vp.m13() - vp.m11(),
                vp.m23() - vp.m21(),
                vp.m33() - vp.m31()
        );

        /*
         * Reverse-Z with GL_ZERO_TO_ONE:
         *
         * near: clipW - clipZ >= 0
         * far:  clipZ >= 0
         *
         * The infinite far plane is omitted.
         */
        setPlane(
                4,
                vp.m03() - vp.m02(),
                vp.m13() - vp.m12(),
                vp.m23() - vp.m22(),
                vp.m33() - vp.m32()
        );
    }

    private void setPlane(
            int index,
            float nx,
            float ny,
            float nz,
            float d
    ) {
        float lengthSquared = nx * nx + ny * ny + nz * nz;

        if (!(lengthSquared > MIN_NORMAL_LENGTH_SQUARED)
                || !Float.isFinite(lengthSquared)) {
            throw new IllegalArgumentException(
                    "Degenerate frustum plane: " + index
            );
        }

        float inverseLength =
                1.0f / (float) Math.sqrt(lengthSquared);

        int base = index * 4;
        planes[base]     = nx * inverseLength;
        planes[base + 1] = ny * inverseLength;
        planes[base + 2] = nz * inverseLength;
        planes[base + 3] = d * inverseLength;
    }

    public int testAab(
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ
    ) {
        tested++;

        boolean intersecting = false;

        for (int plane = 0; plane < PLANE_COUNT; plane++) {
            int base = plane * 4;

            float nx = planes[base];
            float ny = planes[base + 1];
            float nz = planes[base + 2];
            float d  = planes[base + 3];

            /*
             * Vertex farthest along the inward-facing normal.
             * If this is outside, the entire AABB is outside.
             */
            float positiveX = nx >= 0.0f ? maxX : minX;
            float positiveY = ny >= 0.0f ? maxY : minY;
            float positiveZ = nz >= 0.0f ? maxZ : minZ;

            float positiveDistance =
                    nx * positiveX
                            + ny * positiveY
                            + nz * positiveZ
                            + d;

            if (positiveDistance < 0.0f) {
                culled++;
                return OUTSIDE;
            }

            /*
             * Vertex farthest opposite the inward-facing normal.
             * If this is outside, the AABB crosses this plane.
             */
            float negativeX = nx >= 0.0f ? minX : maxX;
            float negativeY = ny >= 0.0f ? minY : maxY;
            float negativeZ = nz >= 0.0f ? minZ : maxZ;

            float negativeDistance =
                    nx * negativeX
                            + ny * negativeY
                            + nz * negativeZ
                            + d;

            if (negativeDistance < 0.0f) {
                intersecting = true;
            }
        }

        return intersecting ? INTERSECT : INSIDE;
    }
}
package com.melon.foolsEngine.util;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

/**
 * Frustum culling for reverse-Z infinite-far projection.
 * <p>
 * LEFT/RIGHT/BOTTOM/TOP use the homogeneous plane method (correct for any depth).
 * NEAR plane is built from NDC corner unprojection (world-space).
 * FAR plane is NOT tested -- the frustum is open-ended at the far side.
 */
public final class RevZFrustumIntersection {

    public static final int OUTSIDE = -1;
    public static final int INTERSECT = 0;
    public static final int INSIDE = 1;

    private static final int PLANE_COUNT = 5;
    private final float[] planes = new float[PLANE_COUNT * 4];

    private int culled;
    private int tested;

    public int culledCount() { return culled; }
    public int testedCount() { return tested; }
    public void resetCounters() { culled = 0; tested = 0; }

    public void setVp(Matrix4fc vp) {
        float m00 = vp.m00(), m01 = vp.m01(), m02 = vp.m02(), m03 = vp.m03();
        float m10 = vp.m10(), m11 = vp.m11(), m12 = vp.m12(), m13 = vp.m13();
        float m20 = vp.m20(), m21 = vp.m21(), m22 = vp.m22(), m23 = vp.m23();
        float m30 = vp.m30(), m31 = vp.m31(), m32 = vp.m32(), m33 = vp.m33();

        // Left:   w + x ≥ 0  (Row4 + Row1)
        p(0, m30 + m00, m31 + m01, m32 + m02, m33 + m03);
        // Right:  w - x ≥ 0  (Row4 - Row1)
        p(1, m30 - m00, m31 - m01, m32 - m02, m33 - m03);
        // Bottom: w + y ≥ 0  (Row4 + Row2)
        p(2, m30 + m10, m31 + m11, m32 + m12, m33 + m13);
        // Top:    w - y ≥ 0  (Row4 - Row2)
        p(3, m30 - m10, m31 - m11, m32 - m12, m33 - m13);

        // Near: world-space plane built from NDC near corners (z=1)
        float[][] nc = {
            {-1, -1, 1, 1}, { 1, -1, 1, 1}, {-1,  1, 1, 1}, { 1,  1, 1, 1},
        };
        Matrix4f inv = new Matrix4f(vp).invert();
        Vector4f t = new Vector4f();
        float[][] ws = new float[4][3];
        for (int i = 0; i < 4; i++) {
            t.set(nc[i][0], nc[i][1], nc[i][2], nc[i][3]);
            t.mul(inv);
            float w = t.w;
            float s = Math.abs(w) > 1e-30f ? 1f / w : 1f;
            ws[i][0] = t.x * s; ws[i][1] = t.y * s; ws[i][2] = t.z * s;
        }
        nearPlane(4, ws[0], ws[2], ws[1]);
    }

    /** Stores a homogeneous plane, normalised. */
    private void p(int idx, float nx, float ny, float nz, float d) {
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 1e-30f) { nx /= len; ny /= len; nz /= len; d /= len; }
        int i = idx * 4;
        planes[i]     = nx;
        planes[i + 1] = ny;
        planes[i + 2] = nz;
        planes[i + 3] = d;
    }

    /** Builds an inward-facing plane from 3 world-space corners (CCW from inside). */
    private void nearPlane(int idx, float[] a, float[] b, float[] c) {
        float bx = b[0] - a[0], by = b[1] - a[1], bz = b[2] - a[2];
        float cx = c[0] - a[0], cy = c[1] - a[1], cz = c[2] - a[2];
        float nx = by * cz - bz * cy;
        float ny = bz * cx - bx * cz;
        float nz = bx * cy - by * cx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 1e-30f) { nx /= len; ny /= len; nz /= len; }
        int i = idx * 4;
        planes[i]     = nx;
        planes[i + 1] = ny;
        planes[i + 2] = nz;
        planes[i + 3] = -(nx * a[0] + ny * a[1] + nz * a[2]);
    }

    public int testAab(float minX, float minY, float minZ,
                        float maxX, float maxY, float maxZ) {
        tested++;
        for (int i = 0; i < PLANE_COUNT; i++) {
            int b = i * 4;
            float nx = planes[b], ny = planes[b + 1], nz = planes[b + 2], d = planes[b + 3];
            float px = nx > 0 ? maxX : minX;
            float py = ny > 0 ? maxY : minY;
            float pz = nz > 0 ? maxZ : minZ;
            if (nx * px + ny * py + nz * pz + d < 0) {
                culled++;
                return OUTSIDE;
            }
        }
        return INSIDE;
    }
}

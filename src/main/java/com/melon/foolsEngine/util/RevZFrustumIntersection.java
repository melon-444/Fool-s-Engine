package com.melon.foolsEngine.util;

import org.joml.Matrix4fc;
import org.joml.Vector3fc;

/**
 * Frustum culling for reverse-Z + {@code glClipControl(GL_LOWER_LEFT, GL_ZERO_TO_ONE)}.
 * <p>
 * Clip-space bounds: -w &le; x &le; w, -w &le; y &le; w, 0 &le; z &le; w (near=1, far=0).
 * Planes are NOT normalised — only the sign of the distance matters for AABB tests.
 */
public final class RevZFrustumIntersection {

    public static final int OUTSIDE = -1;
    public static final int INTERSECT = 0;
    public static final int INSIDE = 1;

    private final float[] planes = new float[24]; // {nx,ny,nz,w} × 6

    private int culled;
    private int tested;

    public int culledCount() { return culled; }
    public int testedCount() { return tested; }
    public void resetCounters() { culled = 0; tested = 0; }

    /**
     * Builds frustum planes from a view-projection matrix.
     */
    public void setVp(Matrix4fc vp) {
        // Column-major access: m[row + col*4] for Matrix4f storage,
        // but Matrix4fc.get(transposed) gives row-major in float[16].
        // Using Matrix4fc getters which are row-major indexed.
        // Row i = (m[i*4], m[i*4+1], m[i*4+2], m[i*4+3]) where m is transposed.
        // Actually, JOML's .get(float[]) returns column-major order.
        // get(col, row) gives element at column,row.
        float[] p = planes;

        float m00 = vp.m00(), m01 = vp.m01(), m02 = vp.m02(), m03 = vp.m03();
        float m10 = vp.m10(), m11 = vp.m11(), m12 = vp.m12(), m13 = vp.m13();
        float m20 = vp.m20(), m21 = vp.m21(), m22 = vp.m22(), m23 = vp.m23();
        float m30 = vp.m30(), m31 = vp.m31(), m32 = vp.m32(), m33 = vp.m33();

        // Left:   Row4 + Row1
        p[0]  = m30 + m00; p[1]  = m31 + m01; p[2]  = m32 + m02; p[3]  = m33 + m03;
        // Right:  Row4 - Row1
        p[4]  = m30 - m00; p[5]  = m31 - m01; p[6]  = m32 - m02; p[7]  = m33 - m03;
        // Bottom: Row4 + Row2
        p[8]  = m30 + m10; p[9]  = m31 + m11; p[10] = m32 + m12; p[11] = m33 + m13;
        // Top:    Row4 - Row2
        p[12] = m30 - m10; p[13] = m31 - m11; p[14] = m32 - m12; p[15] = m33 - m13;
        // Near:   Row4 - Row3  (z_clip ≤ w_clip, reverse-Z near plane)
        p[16] = m30 - m20; p[17] = m31 - m21; p[18] = m32 - m22; p[19] = m33 - m23;
        // Far:    Row3         (z_clip ≥ 0, reverse-Z far plane)
        p[20] = m20;       p[21] = m21;       p[22] = m22;       p[23] = m23;
    }

    /**
     * Returns INSIDE, INTERSECT, or OUTSIDE for a world-space AABB.
     */
    public int testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        tested++;
        float[] p = planes;
        int insideMask = 0x3F; // bits 0-5 = inside on each plane

        for (int i = 0; i < 6; i++) {
            int base = i * 4;
            float nx = p[base], ny = p[base + 1], nz = p[base + 2], d = p[base + 3];

            float px = nx > 0 ? minX : maxX;
            float py = ny > 0 ? minY : maxY;
            float pz = nz > 0 ? minZ : maxZ;

            if (nx * px + ny * py + nz * pz + d < 0) {
                culled++;
                return OUTSIDE;
            }

            // Check if AABB is fully inside this plane's positive half-space.
            // The "furthest" vertex is the opposite corner.
            float qx = nx > 0 ? maxX : minX;
            float qy = ny > 0 ? maxY : minY;
            float qz = nz > 0 ? maxZ : minZ;
            if (nx * qx + ny * qy + nz * qz + d < 0) {
                insideMask &= ~(1 << i);
            }
        }

        return insideMask == 0x3F ? INSIDE : INTERSECT;
    }
}

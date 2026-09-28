package com.melon.snake3d;

import com.melon.foolsEngine.api.rendering.resource.MeshData;
import com.melon.foolsEngine.util.VertexLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Procedurally generates placeholder geometry. A single unit box (1x1x1) is
 * enough for the whole game: every object is that box, scaled/translated by its
 * model matrix. Faces carry outward normals so the flat shader can add a simple
 * directional shade for depth readability.
 */
public final class MeshFactory {

    private MeshFactory() {
    }

    /**
     * @return a box of the given size centered at the origin, 24 vertices
     *         (position + normal, 6 floats each) and 36 indices, wound
     *         counter-clockwise for the renderer's back-face culling.
     */
    public static MeshData box(float width, float height, float depth) {
        float hw = width * 0.5f, hh = height * 0.5f, hd = depth * 0.5f;
        List<Float> positions = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        face(positions, indices, 1f, 0f, 0f, 0f, 1f, 0f, hh, 0f, 0f, 1f, hd, hw, 0f, 0f);
        face(positions, indices, -1f, 0f, 0f, 0f, 0f, 1f, hd, 0f, 1f, 0f, hh, -hw, 0f, 0f);
        face(positions, indices, 0f, 1f, 0f, 0f, 0f, 1f, hd, 1f, 0f, 0f, hw, 0f, hh, 0f);
        face(positions, indices, 0f, -1f, 0f, 1f, 0f, 0f, hw, 0f, 0f, 1f, hd, 0f, -hh, 0f);
        face(positions, indices, 0f, 0f, 1f, 1f, 0f, 0f, hw, 0f, 1f, 0f, hh, 0f, 0f, hd);
        face(positions, indices, 0f, 0f, -1f, 0f, 1f, 0f, hh, 1f, 0f, 0f, hw, 0f, 0f, -hd);

        float[] verts = new float[positions.size()];
        for (int i = 0; i < verts.length; i++) {
            verts[i] = positions.get(i);
        }
        int[] idx = new int[indices.size()];
        for (int i = 0; i < idx.length; i++) {
            idx[i] = indices.get(i);
        }

        VertexLayout layout = new VertexLayout().add(0, 3).add(2, 3);
        return new MeshData(verts, idx, layout);
    }

    /**
     * @return a unit quad (1x1) on the XZ plane facing +Y, with UVs. Used for
     *         billboard-style shader effects such as the portal swirl.
     */
    public static MeshData quad() {
        float[] verts = {
                -0.5f, 0f, -0.5f, 0f, 0f,
                0.5f, 0f, -0.5f, 1f, 0f,
                0.5f, 0f, 0.5f, 1f, 1f,
                -0.5f, 0f, 0.5f, 0f, 1f,
        };
        int[] idx = {0, 2, 1, 0, 3, 2};
        VertexLayout layout = new VertexLayout().add(0, 3).add(1, 2);
        return new MeshData(verts, idx, layout);
    }

    private static void face(List<Float> positions, List<Integer> indices,
                             float nx, float ny, float nz,
                             float ux, float uy, float uz, float eu,
                             float vx, float vy, float vz, float ev,
                             float cx, float cy, float cz) {
        int base = positions.size() / 6;
        vertex(positions, cx - ux * eu - vx * ev, cy - uy * eu - vy * ev, cz - uz * eu - vz * ev, nx, ny, nz);
        vertex(positions, cx + ux * eu - vx * ev, cy + uy * eu - vy * ev, cz + uz * eu - vz * ev, nx, ny, nz);
        vertex(positions, cx + ux * eu + vx * ev, cy + uy * eu + vy * ev, cz + uz * eu + vz * ev, nx, ny, nz);
        vertex(positions, cx - ux * eu + vx * ev, cy - uy * eu + vy * ev, cz - uz * eu + vz * ev, nx, ny, nz);
        indices.add(base);
        indices.add(base + 1);
        indices.add(base + 2);
        indices.add(base);
        indices.add(base + 2);
        indices.add(base + 3);
    }

    private static void vertex(List<Float> positions,
                               float x, float y, float z,
                               float nx, float ny, float nz) {
        positions.add(x);
        positions.add(y);
        positions.add(z);
        positions.add(nx);
        positions.add(ny);
        positions.add(nz);
    }
}

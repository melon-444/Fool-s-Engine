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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class OctreeTest {

    private static final class Sample {
        final float minX, minY, minZ, maxX, maxY, maxZ;
        Sample(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.minX = minX; this.minY = minY; this.minZ = minZ;
            this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        }
    }

    /**
     * The octree is a conservative broad-phase: it must return every item that
     * is {@link RevZFrustumIntersection#INSIDE} the frustum and must never
     * return an item that is {@link RevZFrustumIntersection#OUTSIDE}. Items
     * that merely {@code INTERSECT} may or may not be returned (the per-item
     * test is conservative, while the tree may prune such items more tightly).
     */
    @Test
    void queryMatchesBruteForceAcrossRandomFrustums() {
        Random rng = new Random(123456789L);

        List<Sample> samples = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            float cx = (rng.nextFloat() - 0.5f) * 200f;
            float cy = (rng.nextFloat() - 0.5f) * 200f;
            float cz = (rng.nextFloat() - 0.5f) * 200f;
            float hx = rng.nextFloat() * 20f;
            float hy = rng.nextFloat() * 20f;
            float hz = rng.nextFloat() * 20f;
            samples.add(new Sample(cx - hx, cy - hy, cz - hz, cx + hx, cy + hy, cz + hz));
        }
        // A few large AABBs that span many octants.
        samples.add(new Sample(-200, -200, -200, 200, 200, 200));
        samples.add(new Sample(-50, -50, -50, 50, 50, 50));
        // A degenerate point.
        samples.add(new Sample(10, 10, 10, 10, 10, 10));

        List<Octree.Item> items = new ArrayList<>(samples.size());
        for (int i = 0; i < samples.size(); i++) {
            Sample s = samples.get(i);
            items.add(new Octree.Item(i, s.minX, s.minY, s.minZ, s.maxX, s.maxY, s.maxZ));
        }

        Octree octree = new Octree();
        octree.build(items);

        for (int trial = 0; trial < 40; trial++) {
            float camX = (rng.nextFloat() - 0.5f) * 300f;
            float camY = (rng.nextFloat() - 0.5f) * 300f;
            float camZ = (rng.nextFloat() - 0.5f) * 300f;
            float tx = (rng.nextFloat() - 0.5f) * 100f;
            float ty = (rng.nextFloat() - 0.5f) * 100f;
            float tz = (rng.nextFloat() - 0.5f) * 100f;

            Matrix4f vp = new Matrix4f()
                    .perspective((float) Math.toRadians(60), 1.6f, 0.01f, 1000f, true)
                    .lookAt(camX, camY, camZ, tx, ty, tz, 0, 1, 0);
            RevZFrustumIntersection frustum = new RevZFrustumIntersection();
            frustum.setVp(vp);

            Set<Integer> inside = new HashSet<>();
            Set<Integer> outside = new HashSet<>();
            for (int i = 0; i < samples.size(); i++) {
                Sample s = samples.get(i);
                int r = frustum.testAab(s.minX, s.minY, s.minZ, s.maxX, s.maxY, s.maxZ);
                if (r == RevZFrustumIntersection.INSIDE) inside.add(i);
                else if (r == RevZFrustumIntersection.OUTSIDE) outside.add(i);
            }

            List<Integer> result = new ArrayList<>();
            octree.query(frustum, result);
            Set<Integer> actual = new HashSet<>(result);

            // Every fully-inside item must be reported.
            for (int id : inside) {
                assertTrue(actual.contains(id),
                        "trial " + trial + ": INSIDE item " + id + " was pruned");
            }
            // No fully-outside item may be reported.
            for (int id : outside) {
                assertFalse(actual.contains(id),
                        "trial " + trial + ": OUTSIDE item " + id + " was reported");
            }
        }
    }

    @Test
    void emptyTreeQueriesNothing() {
        Octree octree = new Octree();
        octree.build(new ArrayList<>());
        List<Integer> out = new ArrayList<>();
        Matrix4f vp = new Matrix4f()
                .perspective((float) Math.toRadians(60), 1.6f, 0.01f, 1000f, true)
                .lookAt(0, 0, -5, 0, 0, 0, 0, 1, 0);
        RevZFrustumIntersection f = new RevZFrustumIntersection();
        f.setVp(vp);
        octree.query(f, out);
        assertTrue(out.isEmpty());
    }
}

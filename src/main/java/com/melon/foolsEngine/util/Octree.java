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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A dynamic axis-aligned octree for broad-phase frustum culling.
 *
 * <p>Stores integer payloads (e.g. entity ids) together with their world-space
 * AABBs. It is rebuilt from scratch on each {@link #build} call, then
 * {@link #query} returns every payload whose AABB is not
 * {@link RevZFrustumIntersection#OUTSIDE} of the given frustum.</p>
 *
 * <p>The tree is an <em>exact</em> acceleration structure: its query result
 * matches a brute-force per-AABB frustum test. Items that straddle an octant
 * boundary are inserted into every octant they overlap, so no visible item can
 * ever be pruned. A single payload may therefore appear more than once in the
 * query output; de-duplicate if ids must be unique.</p>
 */
public final class Octree {

    /** A payload id and its world-space axis-aligned bounding box. */
    public record Item(int id, float minX, float minY, float minZ,
                       float maxX, float maxY, float maxZ) {}

    private static final int DEFAULT_MAX_DEPTH = 6;
    private static final int DEFAULT_MAX_OBJECTS = 16;
    private static final float DEGENERATE_EPS = 1e-9f;

    private final int maxDepth;
    private final int maxObjects;

    private Node root;

    public Octree() {
        this(DEFAULT_MAX_DEPTH, DEFAULT_MAX_OBJECTS);
    }

    /**
     * @param maxDepth maximum subdivision depth (must be {@code >= 1})
     * @param maxObjects leaf capacity before a node subdivides (must be {@code >= 1})
     */
    public Octree(int maxDepth, int maxObjects) {
        if (maxDepth < 1) throw new IllegalArgumentException("maxDepth must be >= 1");
        if (maxObjects < 1) throw new IllegalArgumentException("maxObjects must be >= 1");
        this.maxDepth = maxDepth;
        this.maxObjects = maxObjects;
    }

    /** Rebuilds the tree to hold exactly the given items. */
    public void build(List<Item> items) {
        root = null;
        if (items.isEmpty()) return;

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (Item it : items) {
            minX = Math.min(minX, it.minX());
            minY = Math.min(minY, it.minY());
            minZ = Math.min(minZ, it.minZ());
            maxX = Math.max(maxX, it.maxX());
            maxY = Math.max(maxY, it.maxY());
            maxZ = Math.max(maxZ, it.maxZ());
        }

        // Pad degenerate dimensions so subdividing never produces zero-size nodes.
        float spanX = maxX - minX;
        float spanY = maxY - minY;
        float spanZ = maxZ - minZ;
        float maxSpan = Math.max(spanX, Math.max(spanY, spanZ));
        float pad = Math.max(maxSpan * 1e-4f, 1e-4f);
        if (spanX < DEGENERATE_EPS) { minX -= pad; maxX += pad; }
        if (spanY < DEGENERATE_EPS) { minY -= pad; maxY += pad; }
        if (spanZ < DEGENERATE_EPS) { minZ -= pad; maxZ += pad; }

        root = new Node(minX, minY, minZ, maxX, maxY, maxZ);
        for (Item it : items) {
            insert(root, it, 0);
        }
    }

    /**
     * Adds the id of every stored item whose AABB is not
     * {@link RevZFrustumIntersection#OUTSIDE} of {@code frustum} to {@code out}.
     */
    public void query(RevZFrustumIntersection frustum, Collection<Integer> out) {
        if (root == null) return;
        query(root, frustum, out);
    }

    private void insert(Node node, Item item, int depth) {
        if (node.items != null) {
            if (node.items.size() < maxObjects || depth >= maxDepth) {
                node.items.add(item);
                return;
            }
            split(node, depth);
        }
        insertIntoChildren(node, item, depth);
    }

    private void split(Node node, int depth) {
        float midX = (node.minX + node.maxX) * 0.5f;
        float midY = (node.minY + node.maxY) * 0.5f;
        float midZ = (node.minZ + node.maxZ) * 0.5f;

        node.children = new Node[8];
        for (int i = 0; i < 8; i++) {
            node.children[i] = new Node(
                    (i & 1) == 0 ? node.minX : midX,
                    (i & 2) == 0 ? node.minY : midY,
                    (i & 4) == 0 ? node.minZ : midZ,
                    (i & 1) == 0 ? midX : node.maxX,
                    (i & 2) == 0 ? midY : node.maxY,
                    (i & 4) == 0 ? midZ : node.maxZ);
        }

        List<Item> redistributed = node.items;
        node.items = null;
        for (Item it : redistributed) {
            insertIntoChildren(node, it, depth);
        }
    }

    /** Routes an item into every child octant its AABB overlaps. */
    private void insertIntoChildren(Node node, Item item, int depth) {
        float midX = (node.minX + node.maxX) * 0.5f;
        float midY = (node.minY + node.maxY) * 0.5f;
        float midZ = (node.minZ + node.maxZ) * 0.5f;

        int xMin = item.minX() <= midX ? 0 : 1;
        int xMax = item.maxX() >= midX ? 1 : 0;
        int yMin = item.minY() <= midY ? 0 : 1;
        int yMax = item.maxY() >= midY ? 1 : 0;
        int zMin = item.minZ() <= midZ ? 0 : 1;
        int zMax = item.maxZ() >= midZ ? 1 : 0;

        for (int x = xMin; x <= xMax; x++) {
            for (int y = yMin; y <= yMax; y++) {
                for (int z = zMin; z <= zMax; z++) {
                    insert(node.children[(z << 2) | (y << 1) | x], item, depth + 1);
                }
            }
        }
    }

    private void query(Node node, RevZFrustumIntersection frustum, Collection<Integer> out) {
        int result = frustum.testAab(
                node.minX, node.minY, node.minZ, node.maxX, node.maxY, node.maxZ);
        if (result == RevZFrustumIntersection.OUTSIDE) return;

        if (node.items != null) {
            if (result == RevZFrustumIntersection.INSIDE) {
                for (Item it : node.items) out.add(it.id());
            } else {
                for (Item it : node.items) {
                    if (frustum.testAab(it.minX(), it.minY(), it.minZ(),
                            it.maxX(), it.maxY(), it.maxZ()) != RevZFrustumIntersection.OUTSIDE) {
                        out.add(it.id());
                    }
                }
            }
            return;
        }

        for (Node child : node.children) {
            if (child != null) query(child, frustum, out);
        }
    }

    /** A tree node: either a leaf ({@code items != null}) or an internal node. */
    private static final class Node {
        final float minX;
        final float minY;
        final float minZ;
        final float maxX;
        final float maxY;
        final float maxZ;

        Node[] children;
        List<Item> items = new ArrayList<>();

        Node(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
    }
}

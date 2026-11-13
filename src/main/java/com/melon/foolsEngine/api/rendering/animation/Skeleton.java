package com.melon.foolsEngine.api.rendering.animation;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.List;

/** Parent-first node hierarchy, including nodes that do not weight any vertices. */
public record Skeleton(List<Joint> joints) {
    public Skeleton {
        joints = List.copyOf(joints);
        for (int i = 0; i < joints.size(); i++) {
            int parent = joints.get(i).parent();
            if (parent < -1 || parent >= i) {
                throw new IllegalArgumentException("Skeleton parents must precede their children");
            }
        }
    }

    public record Joint(String name, int parent, Matrix4fc bindLocal) {
        public Joint {
            bindLocal = new Matrix4f(bindLocal);
        }

        @Override
        public Matrix4fc bindLocal() {
            return new Matrix4f(bindLocal);
        }
    }
}

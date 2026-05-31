package com.melon.foolsEngine.core.ECS.basicComponents;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;


public class Transform extends Component {

    public final Vector3f position = new Vector3f();
    public final Quaternionf rotation = new Quaternionf();
    public final Vector3f scale = new Vector3f(1, 1, 1);

    // 缓存（避免频繁 new）
    private final Matrix4f matrix = new Matrix4f();
    private boolean dirty = true;

    public Transform() {}

    public Transform(Vector3f position, Quaternionf rotation) {
        this.position.set(position);
        this.rotation.set(rotation);
    }

    public Transform(Vector3f position, Quaternionf rotation, Vector3f scale) {
        this.position.set(position);
        this.rotation.set(rotation);
        this.scale.set(scale);
    }

    public Transform(Vector3f position) {
        this.position.set(position);
    }

    // dirty marker
    public void markDirty() {
        dirty = true;
    }

    // cached get
    public Matrix4f getMatrix() {
        if (dirty) {
            matrix.identity()
                    .translate(position)
                    .rotate(rotation)
                    .scale(scale);
            dirty = false;
        }
        return matrix;
    }
}
//M = t*r*s
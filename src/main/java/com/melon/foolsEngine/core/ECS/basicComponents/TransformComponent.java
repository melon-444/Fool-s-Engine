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

package com.melon.foolsEngine.core.ECS.basicComponents;

import org.joml.*;


public class TransformComponent extends Component {

    private final Vector3f position = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();
    private final Vector3f scale = new Vector3f(1, 1, 1);

    // 缓存（避免频繁 new）
    private final Matrix4f matrix = new Matrix4f();
    private boolean dirty = true;

    public TransformComponent() {}

    public TransformComponent(Vector3f position, Quaternionf rotation) {
        this.getPosition().set(position);
        this.getRotation().set(rotation);
    }

    public TransformComponent(Vector3f position, Quaternionf rotation, Vector3f scale) {
        this.getPosition().set(position);
        this.getRotation().set(rotation);
        this.getScale().set(scale);
    }

    public TransformComponent(Vector3f position) {
        this.getPosition().set(position);
    }

    // dirty marker
    public void markDirty() {
        dirty = true;
    }

    // cached get
    public Matrix4f getMatrix() {
        if (dirty) {
            matrix.identity()
                    .translate(getPosition())
                    .rotate(getRotation())
                    .scale(getScale());
            dirty = false;
        }
        return matrix;
    }

    public void setFromMatrix(Matrix4f matrix) {
        this.matrix.set(matrix);
        decompose(matrix.get(new float[16]));
        dirty = false;
    }

    private void decompose(float[] mat) {

        Matrix3f rotation = new Matrix3f();
        // 1. 提取平移
        getPosition().x = mat[3];
        getPosition().y = mat[7];
        getPosition().z = mat[11];

        // 2. 提取列向量 (注意列主序)
        Vector3f col0 = new Vector3f(mat[0], mat[4], mat[8]);
        Vector3f col1 = new Vector3f(mat[1], mat[5], mat[9]);
        Vector3f col2 = new Vector3f(mat[2], mat[6], mat[10]);

        // 3. 计算缩放
        getScale().x = col0.length();
        getScale().y = col1.length();
        getScale().z = col2.length();

        // 4. 构造旋转矩阵（单位化列）
        rotation.m00 = col0.x / getScale().x; rotation.m01 = col1.x / getScale().y; rotation.m02 = col2.x / getScale().z;
        rotation.m10 = col0.y / getScale().x; rotation.m11 = col1.y / getScale().y; rotation.m12 = col2.y / getScale().z;
        rotation.m20 = col0.z / getScale().x; rotation.m21 = col1.z / getScale().y; rotation.m22 = col2.z / getScale().z;

        // 5. 修复镜像
        if (rotation.determinant() < 0) {
            getScale().z = -getScale().z;
            rotation.m02 = -rotation.m02;
            rotation.m12 = -rotation.m12;
            rotation.m22 = -rotation.m22;
        }
        this.getRotation().setFromNormalized(rotation);
    }

    public Vector3f getPosition() {
        return position;
    }

    public Quaternionf getRotation() {
        return rotation;
    }

    public Vector3f getScale() {
        return scale;
    }

    public void position(Vector3f position) {
        this.position.set(position);
        markDirty();
    }

    public void rotation(Quaternionf rotation) {
        this.rotation.set(rotation);
        markDirty();
    }

    public void scale(Vector3f scale) {
        this.scale.set(scale);
        markDirty();
    }
}
//M = t*r*s
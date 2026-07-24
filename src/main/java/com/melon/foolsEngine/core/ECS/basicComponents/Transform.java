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

    public void setFromMatrix(Matrix4f matrix) {
        this.matrix.set(matrix);
        decompose(matrix.get(new float[16]));
        dirty = false;
    }

    private void decompose(float[] mat) {

        Matrix3f rotation = new Matrix3f();
        // 1. 提取平移
        position.x = mat[3];
        position.y = mat[7];
        position.z = mat[11];

        // 2. 提取列向量 (注意列主序)
        Vector3f col0 = new Vector3f(mat[0], mat[4], mat[8]);
        Vector3f col1 = new Vector3f(mat[1], mat[5], mat[9]);
        Vector3f col2 = new Vector3f(mat[2], mat[6], mat[10]);

        // 3. 计算缩放
        scale.x = col0.length();
        scale.y = col1.length();
        scale.z = col2.length();

        // 4. 构造旋转矩阵（单位化列）
        rotation.m00 = col0.x / scale.x; rotation.m01 = col1.x / scale.y; rotation.m02 = col2.x / scale.z;
        rotation.m10 = col0.y / scale.x; rotation.m11 = col1.y / scale.y; rotation.m12 = col2.y / scale.z;
        rotation.m20 = col0.z / scale.x; rotation.m21 = col1.z / scale.y; rotation.m22 = col2.z / scale.z;

        // 5. 修复镜像
        if (rotation.determinant() < 0) {
            scale.z = -scale.z;
            rotation.m02 = -rotation.m02;
            rotation.m12 = -rotation.m12;
            rotation.m22 = -rotation.m22;
        }
        this.rotation.setFromNormalized(rotation);
    }

}
//M = t*r*s
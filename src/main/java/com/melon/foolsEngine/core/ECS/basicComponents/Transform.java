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

    public void setFromMatrix(Matrix4f matrix) {
        this.matrix.set(matrix);
        if(dirty)
            dirty = false;
    }
}
//M = t*r*s
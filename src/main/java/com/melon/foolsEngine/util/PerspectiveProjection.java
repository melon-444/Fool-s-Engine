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

import org.joml.Matrix4f;

public final class PerspectiveProjection implements Projection {

    public float fov;
    public float aspect;
    public float near;

    public PerspectiveProjection(float fov, float aspect, float near) {
        this.fov = fov;
        this.aspect = aspect;
        this.near = near;
    }

    @Override
    public Matrix4f get(Matrix4f dest) {
        float f = (float) (1.0 / Math.tan(Math.toRadians(fov * 0.5)));

        return dest.set(
                f / aspect, 0, 0, 0,
                0, f, 0, 0,
                0, 0, 0, -1,
                0, 0, near, 0
        );
    }
}

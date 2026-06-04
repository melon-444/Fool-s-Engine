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

public class OrthogonalProjection implements Projection {

    public float right;
    public float top;
    public float near;
    public float far;

    public OrthogonalProjection(float right, float top, float near, float far) {
        this.right = right;
        this.top = top;
        this.near = near;
        this.far = far;
    }

    @Override
    public Matrix4f get(Matrix4f dest) {

        // X/Y: assume symmetric left/right = -right..right and bottom/top = -top..top
        float invX = 1.0f / right;
        float invY = 1.0f / top;

        // Z mapping: project view-space z (camera looks down -Z, so near plane at z = -near,
        // far plane at z = -far) into NDC. This project uses the [0,1] NDC range with
        // reversed-Z (depth test = GL_GREATER, so near -> 1, far -> 0).
        // Solve for m22 and m32 from:
        //   m22 * (-near) + m32 = 1   (near maps to 1)
        //   m22 * (-far)  + m32 = 0   (far  maps to 0)
        // => m22 = 1/(far - near), m32 = far/(far - near)
        float m22 = 1.0f / (far - near);
        float m32 = far * m22;

        return dest.set(
                invX, 0, 0, 0,
                0, invY, 0, 0,
                0, 0, m22, 0,
                0, 0, m32, 1
        );
    }
}

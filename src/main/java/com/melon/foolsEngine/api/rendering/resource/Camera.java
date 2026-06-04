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

package com.melon.foolsEngine.api.rendering.resource;

import org.joml.Matrix4f;

/**
 * Holds view and projection matrices for rendering.
 * The combined view-projection matrix is lazily computed and cached for performance.
 */
public class Camera {
    /** The projection matrix (perspective or orthographic) */
    public Matrix4f projection;
    /** The view (look-at) matrix */
    public Matrix4f view;
    private Matrix4f lastV;
    private Matrix4f lastP;
    private Matrix4f vp;

    /**
     * @param view the view (look-at) matrix
     * @param projection the projection matrix
     */
    public Camera(Matrix4f view, Matrix4f projection) {
        this.view = view;
        this.projection = projection;
    }

    /**
     * Returns the combined view-projection matrix, recomputed only when view or projection change.
     * @return view-projection matrix (projection * view)
     */
    public Matrix4f vp() {
        if(lastV != null&&lastP!=null) {
            if(projection.equals(lastP)&&view.equals(lastV)) {
                return vp;
            }
        }
        lastV = new Matrix4f(view);
        lastP = new Matrix4f(projection);
        vp = new Matrix4f(projection).mul(view);
        return vp;
    }

}
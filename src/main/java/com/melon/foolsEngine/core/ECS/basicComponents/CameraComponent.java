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

import com.melon.foolsEngine.util.ProjectionType;

public class CameraComponent extends Component {
    public ProjectionType projectionType;
    /** Field of view in degrees (perspective only). */
    public float FOVy;
    public float near;
    /** Far plane distance (orthographic only; perspective uses infinite far). */
    public float far;
    /** Vertical half-extent in world-space units (orthographic only). */
    public float orthoSize;
    public boolean active;

    /** When true, this camera is selected by CameraCollector as the scene's main view camera. */
    public boolean isMainCam = true;

    /** Perspective camera with default FOVy. */
    public CameraComponent(float FOVy, float near) {
        this(FOVy, near, true);
    }

    /** Perspective camera. */
    public CameraComponent(float FOVy, float near, boolean active) {
        this.projectionType = ProjectionType.PERSPECTIVE;
        this.FOVy = FOVy;
        this.near = near;
        this.active = active;
    }

    /** Orthographic camera. */
    public CameraComponent(float near, float far, ProjectionType marker, float orthoSize) {
        this(near, far, marker, orthoSize, true);
    }

    /** Orthographic camera with active flag. */
    public CameraComponent(float near, float far, ProjectionType marker, float orthoSize, boolean active) {
        this.projectionType = ProjectionType.ORTHOGRAPHIC;
        this.near = near;
        this.far = far;
        this.orthoSize = orthoSize;
        this.active = active;
    }
}



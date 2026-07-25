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
    public float FOVy;
    public float near;
    //public float far; infinite
    public float orthoSize;
    public boolean active;

    /** When true, this camera is selected by CameraCollector as the scene's main view camera. */
    public boolean isMainCam = true;

    public CameraComponent(float FOV, float near) {
        this(FOV, near, 1f, true);
    }

    public CameraComponent(float FOV, float near, float orthoSize) {
        this(FOV, near,orthoSize, true);
    }

    public CameraComponent(float FOV, float near, float orthoSize, boolean active) {
        this(FOV,near,orthoSize,active,ProjectionType.PERSPECTIVE);
    }

    public CameraComponent(float FOV, float near,  float orthoSize, boolean active, ProjectionType projectionType) {
        this.FOVy = FOV;
        this.near = near;
        this.orthoSize = orthoSize;
        this.active = active;
        this.projectionType = projectionType;
    }
}



package com.melon.foolsEngine.core.ECS.basicComponents;

import com.melon.foolsEngine.util.ProjectionType;

public class CameraComponent extends Component {
    public ProjectionType projectionType;
    public float FOVy;
    public float near;
    //public float far; infinite
    public float orthoSize;
    public boolean active;

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



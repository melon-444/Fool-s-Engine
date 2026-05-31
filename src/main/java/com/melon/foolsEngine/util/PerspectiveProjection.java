package com.melon.foolsEngine.util;

import org.joml.Matrix4f;

public class PerspectiveProjection implements Projection {

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

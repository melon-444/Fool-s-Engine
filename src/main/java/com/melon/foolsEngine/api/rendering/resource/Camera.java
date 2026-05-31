package com.melon.foolsEngine.api.rendering.resource;

import org.joml.Matrix4f;

public class Camera {
    public Matrix4f projection;
    public Matrix4f view;
    private Matrix4f lastV;
    private Matrix4f lastP;
    private Matrix4f vp;

    public Camera(Matrix4f view, Matrix4f projection) {
        this.view = view;
        this.projection = projection;
    }

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
package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.core.ECS.basicComponents.CameraComponent;
import com.melon.foolsEngine.core.ECS.basicComponents.TransformComp;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.SparseSet;
import org.joml.Matrix4f;

public class CameraCollector extends ClientSystem {

    private final SparseSet<CameraComponent> cameras;
    private final SparseSet<TransformComp> transforms;
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f proj = new Matrix4f();

    {
        requiredComponents.add(CameraComponent.class);
        requiredComponents.add(TransformComp.class);
    }

    public CameraCollector(FoolsEngine engine) {
        super(engine);
        cameras = getSparseSet(CameraComponent.class);
        transforms = getSparseSet(TransformComp.class);
    }

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public void update(float dt, RenderScene scene) {
        for (int e : entities) {
            CameraComponent cam = cameras.getComponent(e);
            if (cam == null || !cam.active) continue;

            deactivateOtherCam(e);

            TransformComp t = transforms.getComponent(e);
            if (t == null) continue;

            view.identity().set(t.getMatrix().invert());
            proj.identity();
            com.melon.foolsEngine.util.PerspectiveProjection persp =
                    new com.melon.foolsEngine.util.PerspectiveProjection(cam.FOVy, INSTANCE.aspect, cam.near);
            persp.get(proj);
            scene.setCamera(new Camera(new Matrix4f(view), new Matrix4f(proj)));
            return;
        }
    }

    private void deactivateOtherCam(int exclude) {
        for (int e : entities) {
            if (e == exclude) continue;
            CameraComponent cam = cameras.getComponent(e);
            if (cam != null) cam.active = false;
        }
    }
}

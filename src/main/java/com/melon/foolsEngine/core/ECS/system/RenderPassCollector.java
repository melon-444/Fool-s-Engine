package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.render.ShaderPass;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.shadow.ShadowManager;
import com.melon.foolsEngine.api.rendering.resource.shadow.ShadowPassContext;
import com.melon.foolsEngine.core.ECS.basicComponents.RenderPassComponent;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.SparseSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RenderPassCollector extends ClientSystem {

    private final SparseSet<RenderPassComponent> passComps;

    {
        requiredComponents.add(RenderPassComponent.class);
    }

    @Override
    public int priority() {
        return 1;
    }

    public RenderPassCollector(FoolsEngine engine) {
        super(engine);
        passComps = getSparseSet(RenderPassComponent.class);
    }

    @Override
    public void update(float dt, RenderScene scene) {
        scene.clearPasses();

        generateShadowPasses(scene);

        List<RenderPassComponent> userPasses = new ArrayList<>();
        for (int e : entities) {
            RenderPassComponent pc = passComps.getComponent(e);
            if (pc != null) userPasses.add(pc);
        }
        userPasses.sort(Comparator.comparingInt(p -> p.order));

        for (RenderPassComponent pc : userPasses) {
            scene.submitPass(pc.pass);
        }
    }

    private void generateShadowPasses(RenderScene scene) {
        LightEnvironment lightEnv = scene.getLighting();
        if (lightEnv == null) return;
        ShadowManager sm = lightEnv.getShadowManager();
        Camera mainCamera = scene.getCamera();
        if (sm == null || mainCamera == null) return;

        for (Light light : lightEnv.getLights()) {
            if (!light.castsShadow()) continue;
            ShadowPassContext ctx = sm.prepareShadow(light, mainCamera);
            ShaderPass sp = new ShaderPass(ctx.depthMaterial().shader())
                    .output(ctx.target())
                    .camera(ctx.shadowCamera())
                    .overrideMaterial(ctx.depthMaterial())
                    .arrayLayer(ctx.layer());
            scene.submitPass(sp);
        }
    }
}

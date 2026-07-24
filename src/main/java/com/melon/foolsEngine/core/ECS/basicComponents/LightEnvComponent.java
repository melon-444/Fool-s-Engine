package com.melon.foolsEngine.core.ECS.basicComponents;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.Material;

public class LightEnvComponent extends Component {

    public final LightEnvironment env;

    public LightEnvComponent() {
        this.env = new LightEnvironment();
    }

    public LightEnvComponent(LightEnvironment env) {
        this.env = env;
    }

    public LightEnvComponent(float ambientR, float ambientG, float ambientB) {
        this.env = new LightEnvironment();
        this.env.setAmbient(ambientR, ambientG, ambientB);
    }

    public void enableShadows(RenderTarget shadowArray, Material depthMaterial, int maxLayers) {
        env.enableShadows(shadowArray, depthMaterial, maxLayers);
    }

    public void destroy() {
        env.destroy();
    }
}

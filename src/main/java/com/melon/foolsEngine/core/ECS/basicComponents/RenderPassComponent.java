package com.melon.foolsEngine.core.ECS.basicComponents;

import com.melon.foolsEngine.api.rendering.pipeline.ShaderPass;

/**
 * ECS component wrapping a {@link ShaderPass} for ordered pipeline construction.
 * Collected by {@code RenderPassCollector} and submitted to {@code RenderScene}.
 */
public class RenderPassComponent extends Component {

    public int order;
    public final ShaderPass pass;

    public RenderPassComponent(int order, ShaderPass pass) {
        this.order = order;
        this.pass = pass;
    }

    public static RenderPassComponent color(int order, ShaderPass pass) {
        return new RenderPassComponent(order, pass);
    }
}

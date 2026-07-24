package com.melon.foolsEngine.core.ECS.basicComponents;

import com.melon.foolsEngine.api.rendering.resource.Material;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;

/**
 * Defines a render pass with draw state and ordering.
 * Multiple RenderPassComponent entities define the full render pipeline — collected by {@code RenderPassCollector}
 * into an ordered list consumed by {@code GLRenderFrame}.
 *
 * <p>Example: opaque pass (depth write, no blend) → transparent pass (blend, depth read-only) → HUD.</p>
 */
public class RenderPassComponent extends Component {

    public static final int PASS_OPAQUE       = 0;
    public static final int PASS_TRANSPARENT  = 1;
    public static final int PASS_SKYBOX       = 2;
    public static final int PASS_HUD          = 3;

    public int order;
    public boolean depthWrite;
    public boolean depthTest;
    public boolean blend;
    public int blendSrcFunc;
    public int blendDstFunc;
    public Material overrideMaterial;

    public RenderPassComponent(int order) {
        this.order = order;
        this.depthWrite = true;
        this.depthTest = true;
        this.blend = false;
    }

    public RenderPassComponent opaque(int passOrder) {
        this.order = passOrder;
        this.depthWrite = true;
        this.depthTest = true;
        this.blend = false;
        return this;
    }

    public RenderPassComponent transparent(int passOrder) {
        this.order = passOrder;
        this.depthWrite = false;
        this.depthTest = true;
        this.blend = true;
        this.blendSrcFunc = org.lwjgl.opengl.GL45.GL_SRC_ALPHA;
        this.blendDstFunc = org.lwjgl.opengl.GL45.GL_ONE_MINUS_SRC_ALPHA;
        return this;
    }

    public RenderPassComponent withMaterial(Material mat) {
        this.overrideMaterial = mat;
        return this;
    }
}

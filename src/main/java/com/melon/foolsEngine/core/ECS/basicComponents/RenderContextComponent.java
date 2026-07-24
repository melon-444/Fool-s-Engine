package com.melon.foolsEngine.core.ECS.basicComponents;

import com.melon.foolsEngine.api.rendering.render.RenderFrame;

public class RenderContextComponent extends Component {

    public final RenderFrame frame;

    public RenderContextComponent(RenderFrame frame) {
        this.frame = frame;
    }
}

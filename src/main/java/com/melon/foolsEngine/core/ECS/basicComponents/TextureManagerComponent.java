package com.melon.foolsEngine.core.ECS.basicComponents;

import com.melon.foolsEngine.api.rendering.resource.texture.TextureManager;

public class TextureManagerComponent extends Component {

    public final TextureManager manager;

    public TextureManagerComponent(TextureManager manager) {
        this.manager = manager;
    }
}

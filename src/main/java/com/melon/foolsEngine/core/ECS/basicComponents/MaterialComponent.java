package com.melon.foolsEngine.core.ECS.basicComponents;

import com.melon.foolsEngine.api.rendering.resource.Material;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;

public class MaterialComponent extends Component {

    public final Material material;

    public MaterialComponent(ShaderProgram shader) {
        this.material = new Material(shader);
    }

    public MaterialComponent(Material material) {
        this.material = material;
    }
}

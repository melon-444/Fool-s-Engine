package com.melon.foolsEngine.core.ECS.basicComponents;

import com.melon.foolsEngine.api.rendering.resource.Material;
import com.melon.foolsEngine.api.rendering.resource.Mesh;

public class Renderable extends Component{
    public Mesh mesh;
    public Material material;

    public Renderable(Mesh mesh, Material material) {
        this.mesh = mesh;
        this.material = material;
    }
}

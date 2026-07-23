// foolsEngine - A custom 3D game engine in Java
// Copyright (C) 2026  melon_444
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

package com.melon.foolsEngine.core.ECS.system;

import com.melon.foolsEngine.api.rendering.resource.Material;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.render.RenderCommand;
import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.core.ECS.basicComponents.Renderable;
import com.melon.foolsEngine.core.ECS.basicComponents.Transform;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.SparseSet;

public class RenderableCollector extends ClientSystem {
    private final SparseSet<Transform> transforms;
    private final SparseSet<Renderable> renderables;

    {
        requiredComponents.add(Transform.class);
        requiredComponents.add(Renderable.class);
    }

    public RenderableCollector(FoolsEngine engine) {
        super(engine);
        transforms = getSparseSet(Transform.class);
        renderables = getSparseSet(Renderable.class);
    }

    @Override
    public void update(float dt, RenderScene scene) {
        for (int e : entities) {
            Transform t = transforms.getComponent(e);
            Mesh meshComp = renderables.getComponent(e).mesh;
            Material material = renderables.getComponent(e).material;
            scene.submit(new RenderCommand(meshComp, material, t.getMatrix()));
        }
    }
}

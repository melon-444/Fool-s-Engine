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
import com.melon.foolsEngine.core.ECS.basicComponents.RenderableComp;
import com.melon.foolsEngine.core.ECS.basicComponents.TransformComp;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.SparseSet;

@Deprecated
public class RenderableCollector extends ClientSystem {
    private final SparseSet<TransformComp> transforms;
    private final SparseSet<RenderableComp> renderables;

    {
        requiredComponents.add(TransformComp.class);
        requiredComponents.add(RenderableComp.class);
    }

    public RenderableCollector(FoolsEngine engine) {
        super(engine);
        transforms = getSparseSet(TransformComp.class);
        renderables = getSparseSet(RenderableComp.class);
    }

    @Override
    public void update(float dt, RenderScene scene) {
        super.update(dt, scene);
        for (int e : entities) {
            TransformComp t = transforms.getComponent(e);
            Mesh meshComp = renderables.getComponent(e).mesh;
            Material material = renderables.getComponent(e).material;
            scene.submit(new RenderCommand(meshComp, material, t.getMatrix()));
        }
    }
}

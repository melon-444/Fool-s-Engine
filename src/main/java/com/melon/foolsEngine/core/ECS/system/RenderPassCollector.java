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

import com.melon.foolsEngine.api.rendering.render.RenderScene;
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
        return 4;
    }

    @Override
    public boolean isPinned() {
        return true;
    }

    public RenderPassCollector(FoolsEngine engine) {
        super(engine);
        passComps = getSparseSet(RenderPassComponent.class);
    }

    @Override
    public void update(float dt, RenderScene scene) {
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
}
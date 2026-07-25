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
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.Distribution;
import com.melon.foolsEngine.core.annotation.OnlyIn;

@OnlyIn(Distribution.Client)
public abstract class ClientSystem extends System<RenderScene> {
    public ClientSystem(FoolsEngine engine) {
        super(engine);
    }
    @Override
    public void update(float dt, RenderScene scene){
        if (scene == null) scene = context;
    }
}

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

import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.Distribution;
import com.melon.foolsEngine.core.annotation.OnlyIn;

@OnlyIn(Distribution.Dedicated_Server)
public abstract class ServerSystem<Context> extends System<Context> {
    public ServerSystem(FoolsEngine engine,Context context) {
        super(engine,context);
    }
    @Override
    public void update(float dt, Context ctx){
        if (ctx == null) ctx = context;
    }
}

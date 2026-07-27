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

package com.melon.foolsEngine.core.bootstrap;

import com.melon.foolsEngine.core.ECS.system.CameraCollector;
import com.melon.foolsEngine.core.ECS.system.LightCollector;
import com.melon.foolsEngine.core.ECS.system.LightEnvCollector;
import com.melon.foolsEngine.core.ECS.system.MaterialCollector;
import com.melon.foolsEngine.core.ECS.system.RenderPassCollector;
import com.melon.foolsEngine.core.ECS.system.RenderableCollector;
import com.melon.foolsEngine.core.ECS.system.TextureManagerCollector;
import com.melon.foolsEngine.core.annotation.OnlyIn;
import com.melon.foolsEngine.core.world.SystemManager;
import com.melon.foolsEngine.util.Distribution;

/**
 * Registers the client systems that make up the default engine runtime.
 *
 * <p>The system implementations remain in {@code core.ECS.system}; this class
 * only owns the default registration policy.</p>
 */
@OnlyIn(Distribution.Client)
public final class BuiltinClientSystems {

    private BuiltinClientSystems() {
    }

    public static void register(SystemManager systems) {
        systems.registerSystem(LightEnvCollector.class);
        systems.registerSystem(TextureManagerCollector.class);
        systems.registerSystem(CameraCollector.class);
        systems.registerSystem(LightCollector.class);
        systems.registerSystem(RenderableCollector.class);
        systems.registerSystem(RenderPassCollector.class);
        systems.registerSystem(MaterialCollector.class);
    }
}

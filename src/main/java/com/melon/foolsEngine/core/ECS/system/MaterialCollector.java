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
import com.melon.foolsEngine.core.ECS.basicComponents.MaterialComponent;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.SparseSet;

/**
 * Collects {@link MaterialComponent} entities and manages material bindings.
 *
 * <p>TODO PBR support: material parameter bindings (roughness, metallic, ao maps),
 * shader variant selection, and texture slot management.</p>
 */
public class MaterialCollector extends ClientSystem {

    private final SparseSet<MaterialComponent> materials;

    {
        requiredComponents.add(MaterialComponent.class);
    }

    public MaterialCollector(FoolsEngine engine) {
        super(engine);
        materials = getSparseSet(MaterialComponent.class);
    }

    @Override
    public boolean isPinned() {
        return true;
    }

    @Override
    public void update(float dt, RenderScene scene) {
        // TODO: PBR — iterate materials, bind texture arrays, set per-material UBOs
        // TODO: material instance management (reuse vs per-entity material data)
    }
}

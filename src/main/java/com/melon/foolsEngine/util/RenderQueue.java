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
package com.melon.foolsEngine.util;

/**
 * The render queue a {@link com.melon.foolsEngine.api.rendering.resource.Material}
 * belongs to.
 *
 * <p>A material selects its own pipeline by declaring a queue. A CORE
 * {@link com.melon.foolsEngine.api.rendering.shader.ShaderPass} declares the
 * queue it renders; the renderer only submits commands whose material queue
 * matches the pass's queue. This is the dependency direction: materials pick
 * their pass, not the other way around.</p>
 */
public enum RenderQueue {
    /** Default opaque geometry. Drawn first with depth writes enabled. */
    OPAQUE,
    /** Transparent / blended geometry. Drawn after OPAQUE, back-to-front. */
    TRANSPARENT
}

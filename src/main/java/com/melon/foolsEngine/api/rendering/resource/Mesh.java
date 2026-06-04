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

package com.melon.foolsEngine.api.rendering.resource;

/**
 * A GPU-resident mesh with vertex and index buffers.
 * Obtain an instance via {@link com.melon.foolsEngine.core.world.ServiceFactory#getMesh()}.
 */
public interface Mesh {
    /** Uploads mesh data to the GPU */
    void upload(MeshData data);
    /** Releases GPU resources */
    void destroy();
    /** Binds the mesh's VAO for rendering */
    void bind();
    /** Unbinds the VAO */
    void unbind();
    /** @return the number of indices in the element buffer */
    int indexCount();
}

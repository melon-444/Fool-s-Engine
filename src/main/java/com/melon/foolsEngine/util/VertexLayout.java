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

import java.util.ArrayList;
import java.util.List;

public class VertexLayout {
    public record VertexAttribute(
            int location,   //  shader layout
            int size,       // vec3=3
            int offset
    ) {}

    private final List<VertexAttribute> attributes = new ArrayList<>();
    private int stride = 0;

    public VertexLayout add(int location, int size) {
        attributes.add(new VertexAttribute(location, size, stride));
        stride += size;
        return this;
    }

    public List<VertexAttribute> attributes() {
        return attributes;
    }

    public int stride() {
        return stride;
    }
}

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
package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.core.events.Event;

/** Fired when a texture is uploaded to the GPU. */
public class TextureLoadedEvent extends Event {
    public final Texture texture;

    public TextureLoadedEvent(Texture texture) {
        this.texture = texture;
    }
}

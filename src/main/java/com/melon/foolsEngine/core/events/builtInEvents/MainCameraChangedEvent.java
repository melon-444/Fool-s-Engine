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

import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.core.events.Event;

/** Fired when the main camera changes (set on RenderScene by CameraCollector). */
public class MainCameraChangedEvent extends Event {
    public final Camera camera;

    public MainCameraChangedEvent(Camera camera) {
        this.camera = camera;
    }
}

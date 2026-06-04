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

package com.melon.foolsEngine.api.input;

import java.util.HashMap;
import java.util.Map;

public class ActionMapping {
    private final Map<InputDevice<?>, Map<FoolsEngineKeyCode,Action>> map = new HashMap<>();
    public void register(InputDevice<?> inputDevice) {
        if(map.containsKey(inputDevice)) return;
        map.put(inputDevice, new HashMap<>());
    }

    public void bind(InputDevice<?> inputDevice,FoolsEngineKeyCode id, Action action) {
        if(!map.containsKey(inputDevice)) return;
        map.get(inputDevice).put(id, action);
    }

    public Map<FoolsEngineKeyCode,Action> get(InputDevice<?> inputDevice) {
        if(!map.containsKey(inputDevice)) return null;
        return map.get(inputDevice);
    }
}

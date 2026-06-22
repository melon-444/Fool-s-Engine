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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionMapping {
    private final Map<InputDevice<?>, Map<FoolsEngineKeyCode, List<Action>>> forwardMap = new HashMap<>();
    private final Map<InputDevice<?>, Map<Action, FoolsEngineKeyCode>> reverseMap = new HashMap<>();

    public void register(InputDevice<?> inputDevice) {
        if (forwardMap.containsKey(inputDevice)) return;
        forwardMap.put(inputDevice, new HashMap<>());
        reverseMap.put(inputDevice, new HashMap<>());
    }

    public void bind(InputDevice<?> inputDevice, FoolsEngineKeyCode id, Action action) {
        if (!forwardMap.containsKey(inputDevice)) return;

        Map<Action, FoolsEngineKeyCode> rev = reverseMap.get(inputDevice);
        Map<FoolsEngineKeyCode, List<Action>> fwd = forwardMap.get(inputDevice);

        // remove action from its previous key binding
        FoolsEngineKeyCode oldKey = rev.get(action);
        if (oldKey != null) {
            List<Action> oldList = fwd.get(oldKey);
            if (oldList != null) {
                oldList.remove(action);
                if (oldList.isEmpty()) {
                    fwd.remove(oldKey);
                }
            }
        }

        // add action to the new key
        fwd.computeIfAbsent(id, k -> new ArrayList<>()).add(action);
        rev.put(action, id);
    }

    public Map<FoolsEngineKeyCode, List<Action>> get(InputDevice<?> inputDevice) {
        return forwardMap.get(inputDevice);
    }
}

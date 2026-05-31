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

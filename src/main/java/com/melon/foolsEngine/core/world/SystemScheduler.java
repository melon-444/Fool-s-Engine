package com.melon.foolsEngine.core.world;

import java.util.ArrayList;
import java.util.List;

import com.melon.foolsEngine.core.ECS.system.System;
import com.melon.foolsEngine.core.FoolsEngine;

public class SystemScheduler {
    private final List<System> systems = new ArrayList<>();
    private final FoolsEngine Instance;
    private long dt = 1;

    public SystemScheduler(FoolsEngine engine) {
        this.Instance = engine;
        systems.addAll(Instance.systemManager.getRegisteredSystems().values());
    }

    public void checkRegisteredSystem() {
        for (System system : Instance.systemManager.getRegisteredSystems().values()) {
            if(!systems.contains(system))
                systems.add(system);
        }
    }

    public void update(){
        for(System s : systems){
            s.update(dt);
        }
    }
}

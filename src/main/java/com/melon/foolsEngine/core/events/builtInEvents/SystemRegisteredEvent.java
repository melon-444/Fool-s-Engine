package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.core.events.Event;
import com.melon.foolsEngine.core.world.SystemManager;

public class SystemRegisteredEvent extends Event {
    public final SystemManager systemManager;
    public SystemRegisteredEvent(SystemManager systemManager) {
        this.systemManager = systemManager;
    }
}

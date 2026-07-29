package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.core.ECS.system.System;
import com.melon.foolsEngine.core.events.Event;

/** Fired when a system is unregistered from the SystemManager. */
public class SystemUnregisteredEvent extends Event {
    public final Class<? extends System> systemClass;

    public SystemUnregisteredEvent(Class<? extends System> systemClass) {
        this.systemClass = systemClass;
    }
}

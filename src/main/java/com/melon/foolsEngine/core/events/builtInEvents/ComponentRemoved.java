package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.core.ECS.basicComponents.Component;
import com.melon.foolsEngine.core.events.Event;

/** Fired when a component is removed from an entity. */
public class ComponentRemoved extends Event {
    public final int entityId;
    public final Component component;

    public ComponentRemoved(int entityId, Component component) {
        this.entityId = entityId;
        this.component = component;
    }
}

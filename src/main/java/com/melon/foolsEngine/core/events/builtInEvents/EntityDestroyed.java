package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.core.events.Event;

/** Fired when an entity is destroyed. */
public class EntityDestroyed extends Event {
    public final int entityId;

    public EntityDestroyed(int entityId) {
        this.entityId = entityId;
    }
}

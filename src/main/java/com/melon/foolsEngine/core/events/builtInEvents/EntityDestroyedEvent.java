package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.core.events.Event;

/** Fired when an entity is destroyed. */
public class EntityDestroyedEvent extends Event {
    public final int entityId;

    public EntityDestroyedEvent(int entityId) {
        this.entityId = entityId;
    }
}

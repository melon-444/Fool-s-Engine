package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.core.events.Event;

/** Fired when a light is removed from the LightEnvironment. */
public class LightRemovedEvent extends Event {
    public final int entityId;
    public final Light light;

    public LightRemovedEvent(int entityId, Light light) {
        this.entityId = entityId;
        this.light = light;
    }
}

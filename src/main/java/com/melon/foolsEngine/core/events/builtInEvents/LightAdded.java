package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.core.events.Event;

/** Fired when a light is added to the LightEnvironment. */
public class LightAddedEvent extends Event {
    public final int entityId;
    public final Light light;

    public LightAddedEvent(int entityId, Light light) {
        this.entityId = entityId;
        this.light = light;
    }
}

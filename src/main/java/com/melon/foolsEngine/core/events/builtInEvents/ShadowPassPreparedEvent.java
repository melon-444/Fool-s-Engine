package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.api.rendering.resource.shadow.ShadowPassContext;
import com.melon.foolsEngine.core.events.Event;

/** Fired after a shadow pass context is prepared for a light. */
public class ShadowPassPreparedEvent extends Event {
    public final Light light;
    public final ShadowPassContext context;

    public ShadowPassPreparedEvent(Light light, ShadowPassContext context) {
        this.light = light;
        this.context = context;
    }
}

package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.core.events.Event;

/** Fired when a shadow map layer is allocated. */
public class ShadowLayerAllocatedEvent extends Event {
    public final int layer;

    public ShadowLayerAllocatedEvent(int layer) {
        this.layer = layer;
    }
}

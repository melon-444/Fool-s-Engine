package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.core.events.Event;

/** Fired when a shadow map layer is released back to the free list. */
public class ShadowLayerReleasedEvent extends Event {
    public final int layer;

    public ShadowLayerReleasedEvent(int layer) {
        this.layer = layer;
    }
}

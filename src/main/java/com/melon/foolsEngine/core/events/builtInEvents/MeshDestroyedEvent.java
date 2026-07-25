package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.core.events.Event;

/** Fired when mesh GPU resources are freed. */
public class MeshDestroyedEvent extends Event {
    public final Mesh mesh;

    public MeshDestroyedEvent(Mesh mesh) {
        this.mesh = mesh;
    }
}

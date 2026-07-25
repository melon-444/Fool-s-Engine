package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.core.events.Event;

/** Fired when mesh data is uploaded to the GPU. */
public class MeshUploadedEvent extends Event {
    public final Mesh mesh;

    public MeshUploadedEvent(Mesh mesh) {
        this.mesh = mesh;
    }
}

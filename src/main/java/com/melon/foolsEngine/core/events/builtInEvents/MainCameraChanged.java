package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.core.events.Event;

/** Fired when the main camera changes (set on RenderScene by CameraCollector). */
public class MainCameraChanged extends Event {
    public final Camera camera;

    public MainCameraChanged(Camera camera) {
        this.camera = camera;
    }
}

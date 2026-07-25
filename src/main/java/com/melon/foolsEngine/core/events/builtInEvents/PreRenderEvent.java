package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.core.events.Event;

/** Fired immediately before {@code frame.render(scene)}. */
public class PreRenderEvent extends Event {
    public final RenderScene scene;

    public PreRenderEvent(RenderScene scene) {
        this.scene = scene;
    }
}

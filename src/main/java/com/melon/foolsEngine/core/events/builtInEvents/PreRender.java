package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.core.events.Event;

/** Fired immediately before {@code frame.render(scene)}. */
public class PreRender extends Event {
    public final RenderScene scene;

    public PreRender(RenderScene scene) {
        this.scene = scene;
    }
}

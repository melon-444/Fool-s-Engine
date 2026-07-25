package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.core.events.Event;

/** Fired immediately after {@code frame.render(scene)} completes. */
public class PostRenderEvent extends Event {
    public final RenderScene scene;

    public PostRenderEvent(RenderScene scene) {
        this.scene = scene;
    }
}

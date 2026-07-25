package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.core.events.Event;

/** Fired immediately after {@code frame.render(scene)} completes. */
public class PostRender extends Event {
    public final RenderScene scene;

    public PostRender(RenderScene scene) {
        this.scene = scene;
    }
}

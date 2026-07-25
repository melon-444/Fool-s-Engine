package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.core.events.Event;

/** Fired when a texture is destroyed (GPU resources freed). */
public class TextureDestroyedEvent extends Event {
    public final Texture texture;

    public TextureDestroyedEvent(Texture texture) {
        this.texture = texture;
    }
}

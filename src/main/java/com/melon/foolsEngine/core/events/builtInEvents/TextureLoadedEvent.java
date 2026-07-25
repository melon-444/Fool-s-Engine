package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.core.events.Event;

/** Fired when a texture is uploaded to the GPU. */
public class TextureLoadedEvent extends Event {
    public final Texture texture;

    public TextureLoadedEvent(Texture texture) {
        this.texture = texture;
    }
}

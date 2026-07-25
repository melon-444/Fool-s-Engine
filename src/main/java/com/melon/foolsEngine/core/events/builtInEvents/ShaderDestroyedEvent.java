package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.core.events.Event;

/** Fired when a shader program is destroyed (GPU resources freed). */
public class ShaderDestroyedEvent extends Event {
    public final ShaderProgram shader;

    public ShaderDestroyedEvent(ShaderProgram shader) {
        this.shader = shader;
    }
}

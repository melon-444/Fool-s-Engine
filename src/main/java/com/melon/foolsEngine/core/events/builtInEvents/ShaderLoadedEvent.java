package com.melon.foolsEngine.core.events.builtInEvents;

import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.core.events.Event;

/** Fired when a shader program is compiled and linked. */
public class ShaderLoadedEvent extends Event {
    public final ShaderProgram shader;

    public ShaderLoadedEvent(ShaderProgram shader) {
        this.shader = shader;
    }
}

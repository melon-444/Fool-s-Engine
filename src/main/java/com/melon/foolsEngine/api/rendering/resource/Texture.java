package com.melon.foolsEngine.api.rendering.resource;

import java.nio.file.Path;

public interface Texture {
    void upload(Path texture);
    void destroy();
    void bind(int slot);
    void unbind();
}

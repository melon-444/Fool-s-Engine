package com.melon.foolsEngine.api.rendering.render;

public interface RenderTarget {

    int TARGET_COLOR = 0;
    int TARGET_DEPTH = 1;

    void init(int width, int height, int type);

    void bind();

    void unbind();

    int getWidth();

    int getHeight();

    int getTextureId();

    int getType();

    void destroy();
}

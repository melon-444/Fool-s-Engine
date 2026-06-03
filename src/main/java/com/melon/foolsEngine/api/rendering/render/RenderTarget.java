package com.melon.foolsEngine.api.rendering.render;

public interface RenderTarget {

    int TARGET_COLOR = 0;
    int TARGET_DEPTH = 1;

    void init(int width, int height, int type);

    default void init(int width, int height, int type, int layers) {
        init(width, height, type);
    }

    void bind();

    void unbind();

    default void attachLayer(int layer) {
    }

    default int getLayers() {
        return 1;
    }

    int getWidth();

    int getHeight();

    int getTextureId();

    int getType();

    void destroy();
}

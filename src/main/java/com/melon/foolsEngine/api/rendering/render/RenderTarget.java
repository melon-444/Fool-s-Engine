package com.melon.foolsEngine.api.rendering.render;

/**
 * An off-screen render target (FBO).
 * Supports color and depth-only types, with optional layered rendering for texture arrays.
 * <p>
 * Create via {@link com.melon.foolsEngine.core.world.ServiceFactory#createRenderTarget(int, int, int)}:
 * <pre>{@code
 *   RenderTarget rt = factory.createRenderTarget(1024, 1024, RenderTarget.TARGET_DEPTH, 16);
 * }</pre>
 */
public interface RenderTarget {

    /** Color render target type */
    int TARGET_COLOR = 0;
    /** Depth-only render target type (used for shadow maps) */
    int TARGET_DEPTH = 1;

    /** Initializes the render target with the given dimensions and type */
    void init(int width, int height, int type);

    /** Initializes a layered render target (e.g., 2D texture array) */
    default void init(int width, int height, int type, int layers) {
        init(width, height, type);
    }

    /** Binds this render target for subsequent draw calls */
    void bind();

    /** Unbinds the render target, restoring the default framebuffer */
    void unbind();

    /** Attaches a specific layer for rendering (for array targets) */
    default void attachLayer(int layer) {
    }

    /** @return the number of layers (1 for non-array targets) */
    default int getLayers() {
        return 1;
    }

    /** @return the width in pixels */
    int getWidth();

    /** @return the height in pixels */
    int getHeight();

    /** @return the OpenGL texture ID */
    int getTextureId();

    /** @return {@link #TARGET_COLOR} or {@link #TARGET_DEPTH} */
    int getType();

    /** Releases GPU resources */
    void destroy();
}

package com.melon.foolsEngine.api.rendering.resource;

import java.nio.ByteBuffer;

/**
 * Holds pixel data loaded from an image file, along with dimensions and a cleanup callback.
 * <p>
 * Created internally by {@link com.melon.foolsEngine.backend.OpenGL.GLTextureManager}
 * during upload and returned via {@link Texture#getImage()}. The {@link #free()} method
 * must be called to release the underlying native memory when the image is no longer needed;
 * this is handled automatically when the owning {@link Texture} is destroyed.
 */
public final class LoadedImage {
    private final ByteBuffer pixels;
    private final int width;
    private final int height;
    private final Runnable closer;

    /**
     * @param pixels raw RGBA pixel data (native memory, ownership transferred to this object)
     * @param width  image width in pixels
     * @param height image height in pixels
     * @param closer cleanup callback invoked by {@link #free()} to release the pixel buffer
     */
    public LoadedImage(ByteBuffer pixels, int width, int height, Runnable closer) {
        this.pixels = pixels;
        this.width = width;
        this.height = height;
        this.closer = closer;
        if (closer == null) throw new NullPointerException("Missing closer.");
    }

    /** @return the raw RGBA pixel buffer (native memory, do not free externally) */
    public ByteBuffer pixels() { return pixels; }

    /** @return image width in pixels */
    public int width() { return width; }

    /** @return image height in pixels */
    public int height() { return height; }

    /** Releases the underlying pixel buffer. Safe to call multiple times. */
    public void free() {
        if (closer != null) closer.run();
    }
}
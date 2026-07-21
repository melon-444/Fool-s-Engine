// foolsEngine - A custom 3D game engine in Java
// Copyright (C) 2026  melon_444
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
package com.melon.foolsEngine.api.rendering.resource.texture;

import java.nio.ByteBuffer;

/**
 * Holds pixel data loaded from an image file, along with dimensions and a cleanup callback.
 * <p>
 * Created internally by {@link com.melon.foolsEngine.backend}
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
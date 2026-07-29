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
package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.util.ImageFormatDetector;
import com.melon.foolsEngine.util.LoadMode;
import com.melon.foolsEngine.api.rendering.resource.texture.LoadedImage;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.api.rendering.resource.texture.TextureManager;
import com.melon.foolsEngine.core.events.EventBus;
import com.melon.foolsEngine.core.events.builtInEvents.TextureDestroyedEvent;
import com.melon.foolsEngine.core.events.builtInEvents.TextureLoadedEvent;
import com.melon.foolsEngine.util.WrapMode;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.opengl.GL43.*;

class GLTextureManager implements TextureManager {

    private final int arrayTexId;
    private final int width;
    private final int height;
    private final int maxLayers;
    private final Set<Integer> freeLayers = new HashSet<>();
    private final Map<Integer, GLArrayTexture> textureMap = new HashMap<>();
    private final ByteBuffer blankLayer;
    private boolean mipmapDirty;

    private static final int PLACEHOLDER_LAYER = 0;
    private static final int RGBA_BYTES = 4;
    private final GLArrayTexture placeholder;
    private int lastBoundSlot = -1;

    GLTextureManager(int width, int height, int maxLayers) {
        this.width = width;
        this.height = height;
        this.maxLayers = maxLayers;

        arrayTexId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D_ARRAY, arrayTexId);

        int mipLevels = 1 + (int) Math.floor(Math.log(Math.max(width, height)) / Math.log(2));
        glTexStorage3D(GL_TEXTURE_2D_ARRAY, mipLevels, GL_RGBA8, width, height, maxLayers);

        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        int layerBytes = width * height * RGBA_BYTES;
        blankLayer = MemoryUtil.memAlloc(layerBytes);
        try {
            for (int i = 0; i < layerBytes; i++) blankLayer.put(i, (byte) 0);
        } catch (Exception e) {
            MemoryUtil.memFree(blankLayer);
            throw e;
        }

        for (int i = 1; i < maxLayers; i++) freeLayers.add(i);

        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);

        uploadPlaceholder();
        placeholder = new GLArrayTexture(this, PLACEHOLDER_LAYER);
    }

    private void uploadPlaceholder() {
        byte[] white = { (byte) 255, (byte) 255, (byte) 255, (byte) 255 };
        ByteBuffer buf = MemoryUtil.memAlloc(RGBA_BYTES);
        try {
            buf.put(white).flip();
            glBindTexture(GL_TEXTURE_2D_ARRAY, arrayTexId);
            glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, PLACEHOLDER_LAYER, 1, 1, 1,
                    GL_RGBA, GL_UNSIGNED_BYTE, buf);
        } finally {
            MemoryUtil.memFree(buf);
        }
    }

    // ---- upload overloads ----

    @Override
    public Texture upload(Path path) {
        return upload(path, LoadMode.STRETCH, WrapMode.CLAMP_TO_BORDER);
    }

    @Override
    public Texture upload(Path path, LoadMode mode) {
        return upload(path, mode, WrapMode.CLAMP_TO_BORDER);
    }

    @Override
    public Texture upload(Path path, LoadMode mode, WrapMode wrap) {
        int layer = allocateLayer();
        return createTrackedTexture(layer, uploadInternal(path, layer, mode, wrap));
    }

    @Override
    public Texture upload(Path path, int layer) {
        return upload(path, layer, LoadMode.STRETCH, WrapMode.CLAMP_TO_BORDER);
    }

    @Override
    public Texture upload(Path path, int layer, LoadMode mode, WrapMode wrap) {
        releaseCache(layer);
        return createTrackedTexture(layer, uploadInternal(path, layer, mode, wrap));
    }

    // ---- core upload ----

    private ByteBuffer uploadInternal(Path path, int layer, LoadMode mode, WrapMode wrap) {
        byte[] data;
        try {
            data = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load texture: " + e);
        }

        var format = ImageFormatDetector.detect(data);
        ByteBuffer rawImage;
        int srcW, srcH;

        if (!ImageFormatDetector.isStbSupported(format)) {
            var result = GLImageIOLoader.load(data);
            if (result == null) {
                throw new RuntimeException(
                        "Failed to load texture: unsupported format " + format + " — " + path);
            }
            rawImage = result.pixels();
            srcW = result.width();
            srcH = result.height();
        } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                STBImage.stbi_set_flip_vertically_on_load(true);
                ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
                try {
                    buffer.put(data).flip();
                    rawImage = STBImage.stbi_load_from_memory(buffer, w, h, channels, RGBA_BYTES);
                } finally {
                    MemoryUtil.memFree(buffer);
                }

                if (rawImage == null) {
                    var result = GLImageIOLoader.load(data);
                    if (result == null) {
                        throw new RuntimeException(
                                "Failed to load texture: " + STBImage.stbi_failure_reason());
                    }
                    rawImage = result.pixels();
                    srcW = result.width();
                    srcH = result.height();
                } else {
                    srcW = w.get();
                    srcH = h.get();
                }
            }
        }

        ByteBuffer liveImage = rawImage;
        if (srcW != width || srcH != height) {
            liveImage = processSize(rawImage, srcW, srcH, mode, wrap);
            MemoryUtil.memFree(rawImage);
        }
        glBindTexture(GL_TEXTURE_2D_ARRAY, arrayTexId);
        glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, layer, width, height, 1,
                GL_RGBA, GL_UNSIGNED_BYTE, liveImage);
        mipmapDirty = true;
        return liveImage;
    }

    // ---- CPU size processing ----

    private ByteBuffer processSize(ByteBuffer src, int srcW, int srcH, LoadMode mode, WrapMode wrap) {
        return switch (mode) {
            case STRETCH -> stretchNearest(src, srcW, srcH);
            case CROP_WRAP -> cropOrWrap(src, srcW, srcH, wrap);
            case STRICT -> throw new IllegalArgumentException(
                    "Texture size mismatch: expected " + width + "x" + height +
                    ", got " + srcW + "x" + srcH);
        };
    }

    private ByteBuffer stretchNearest(ByteBuffer src, int srcW, int srcH) {
        ByteBuffer dst = MemoryUtil.memAlloc(width * height * RGBA_BYTES);
        try {
            float scaleX = (float) srcW / width;
            float scaleY = (float) srcH / height;
            byte[] row = new byte[width * RGBA_BYTES];
            for (int y = 0; y < height; y++) {
                int srcY = (int) (y * scaleY);
                int srcRowOffset = srcY * srcW * RGBA_BYTES;
                for (int x = 0; x < width; x++) {
                    int srcX = (int) (x * scaleX);
                    int srcOffset = srcRowOffset + srcX * RGBA_BYTES;
                    System.arraycopy(
                            readPixel(src, srcOffset),
                            0, row, x * RGBA_BYTES, RGBA_BYTES);
                }
                dst.put(row);
            }
            dst.flip();
        } catch (Exception e) {
            MemoryUtil.memFree(dst);
            throw e;
        }
        return dst;
    }

    private ByteBuffer cropOrWrap(ByteBuffer src, int srcW, int srcH, WrapMode wrap) {
        ByteBuffer dst = MemoryUtil.memAlloc(width * height * RGBA_BYTES);
        try {
            int copyW = Math.min(srcW, width);
            int copyH = Math.min(srcH, height);

            for (int y = 0; y < copyH; y++) {
                src.position(y * srcW * RGBA_BYTES);
                byte[] row = new byte[copyW * RGBA_BYTES];
                src.get(row);
                dst.position(y * width * RGBA_BYTES);
                dst.put(row);
            }

            int startY = (srcH < height) ? srcH : height;
            for (int y = startY; y < height; y++) {
                fillRow(dst, y * width * RGBA_BYTES, src, srcW, srcH, y, wrap);
            }

            for (int y = 0; y < Math.min(height, srcH); y++) {
                if (srcW < width) {
                    fillColumn(dst, y, srcW, src, srcW, srcH, y, wrap);
                }
            }

            dst.position(0);
        } catch (Exception e) {
            MemoryUtil.memFree(dst);
            throw e;
        }
        return dst;
    }

    private void fillRow(ByteBuffer dst, int dstOffset, ByteBuffer src,
                          int srcW, int srcH, int dstY, WrapMode wrap) {
        byte[] row = new byte[width * RGBA_BYTES];
        if (dstY >= srcH) {
            for (int x = 0; x < width; x++) {
                int sx, sy = wrapCoord(dstY, srcH, wrap);
                sx = (x < srcW) ? x : wrapCoord(x, srcW, wrap);
                int srcOffset = (sy * srcW + sx) * RGBA_BYTES;
                System.arraycopy(readPixel(src, srcOffset), 0,
                        row, x * RGBA_BYTES, RGBA_BYTES);
            }
        }
        dst.position(dstOffset);
        dst.put(row);
    }

    private void fillColumn(ByteBuffer dst, int dstY, int dstStartX, ByteBuffer src,
                             int srcW, int srcH, int rowDstY, WrapMode wrap) {
        byte[] pixel = new byte[RGBA_BYTES];
        int rowBase = dstY * width * RGBA_BYTES;
        for (int x = dstStartX; x < width; x++) {
            int sx = wrapCoord(x, srcW, wrap);
            int sy = wrapCoord(rowDstY, srcH, wrap);
            int srcOffset = (sy * srcW + sx) * RGBA_BYTES;
            System.arraycopy(readPixel(src, srcOffset), 0, pixel, 0, RGBA_BYTES);
            dst.position(rowBase + x * RGBA_BYTES);
            dst.put(pixel);
        }
    }

    private int wrapCoord(int v, int size, WrapMode wrap) {
        return switch (wrap) {
            case CLAMP_TO_BORDER -> -1; // signal to fill with zero
            case REPEAT -> v % size;
            case MIRRORED_REPEAT -> {
                int tile = v / size;
                int rem = v % size;
                yield (tile & 1) == 0 ? rem : size - 1 - rem;
            }
            case CLAMP_TO_EDGE -> Math.min(v, size - 1);
        };
    }

    private byte[] readPixel(ByteBuffer src, int offset) {
        byte[] pixel = new byte[RGBA_BYTES];
        int saved = src.position();
        src.position(offset);
        src.get(pixel);
        src.position(saved);
        return pixel;
    }

    private GLArrayTexture createTrackedTexture(int layer, ByteBuffer pixels) {
        GLArrayTexture tex = new GLArrayTexture(this, layer,
                new LoadedImage(pixels, width, height, () -> MemoryUtil.memFree(pixels)));
        textureMap.put(layer, tex);
        EventBus bus = EventBus.get("SystemBus");
        if (bus != null) bus.emit(new TextureLoadedEvent(tex));
        return tex;
    }

    private void releaseCache(int layer) {
        GLArrayTexture old = textureMap.remove(layer);
        if (old != null) {
            EventBus bus = EventBus.get("SystemBus");
            if (bus != null) bus.emit(new TextureDestroyedEvent(old));
            if (old.getImage() != null) {
                old.getImage().free();
            }
        }
    }

    // ---- rest ----

    @Override
    public Texture getPlaceholder() {
        return placeholder;
    }

    @Override
    public void releaseLayer(int layer) {
        freeLayer(layer);
    }

    void freeLayer(int layer) {
        if (layer <= 0) return;
        releaseCache(layer);
        glBindTexture(GL_TEXTURE_2D_ARRAY, arrayTexId);
        glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, layer, width, height, 1,
                GL_RGBA, GL_UNSIGNED_BYTE, blankLayer);
        freeLayers.add(layer);
    }

    @Override
    public void flushMipmaps() {
        if (mipmapDirty) {
            glBindTexture(GL_TEXTURE_2D_ARRAY, arrayTexId);
            glGenerateMipmap(GL_TEXTURE_2D_ARRAY);
            mipmapDirty = false;
        }
    }

    @Override
    public void bind(int slot) {
        if (lastBoundSlot == slot) return;
        lastBoundSlot = slot;
        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_2D_ARRAY, arrayTexId);
    }

    @Override
    public int getWidth()  { return width; }
    @Override
    public int getHeight() { return height; }
    @Override
    public int getMaxLayers() { return maxLayers; }

    @Override
    public int getActiveLayerCount() {
        return textureMap.size();
    }

    @Override
    public Texture getTexture(int layer) {
        return textureMap.get(layer);
    }

    @Override
    public List<Texture> getTextures() {
        return List.copyOf(textureMap.values());
    }

    @Override
    public void free(Texture texture) {
        freeLayer(texture.getLayer());
    }

    @Override
    public void destroy() {
        releaseCache(PLACEHOLDER_LAYER);
        for (GLArrayTexture tex : textureMap.values()) {
            tex.destroy();
        }
        textureMap.clear();
        freeLayers.clear();
        glDeleteTextures(arrayTexId);
        MemoryUtil.memFree(blankLayer);
    }

    private int allocateLayer() {
        if (freeLayers.isEmpty()) {
            throw new IllegalStateException("Texture layer limit exceeded: " + maxLayers);
        }
        int layer = freeLayers.iterator().next();
        freeLayers.remove(layer);
        return layer;
    }
}

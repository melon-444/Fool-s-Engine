package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.rendering.resource.Texture;
import com.melon.foolsEngine.api.rendering.resource.TextureManager;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.lwjgl.opengl.GL43.*;

class GLTextureManager implements TextureManager {

    private final int arrayTexId;
    private final int width;
    private final int height;
    private final int maxLayers;
    private final Set<Integer> freeLayers = new HashSet<>();
    private int nextLayer;
    private boolean mipmapDirty;

    private static final int PLACEHOLDER_LAYER = 0;
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

        glBindTexture(GL_TEXTURE_2D_ARRAY, 0);

        uploadPlaceholder();
        nextLayer = 1;
        placeholder = new GLArrayTexture(this, PLACEHOLDER_LAYER);
    }

    private void uploadPlaceholder() {
        byte[] white = { (byte) 255, (byte) 255, (byte) 255, (byte) 255 };
        ByteBuffer buf = MemoryUtil.memAlloc(4);
        try {
            buf.put(white).flip();
            glBindTexture(GL_TEXTURE_2D_ARRAY, arrayTexId);
            glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, PLACEHOLDER_LAYER, 1, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        } finally {
            MemoryUtil.memFree(buf);
        }
    }

    @Override
    public Texture upload(Path path) {
        int layer = allocateLayer();
        uploadInternal(path, layer);
        return new GLArrayTexture(this, layer);
    }

    @Override
    public Texture upload(Path path, int layer) {
        uploadInternal(path, layer);
        return new GLArrayTexture(this, layer);
    }

    private void uploadInternal(Path path, int layer) {
        ByteBuffer image;
        int imgWidth, imgHeight;
        try (MemoryStack stack = MemoryStack.stackPush();
             FileInputStream fis = new FileInputStream(path.toFile())) {

            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(true);
            byte[] data = fis.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
            try {
                buffer.put(data).flip();
                image = STBImage.stbi_load_from_memory(buffer, w, h, channels, 4);
            } finally {
                MemoryUtil.memFree(buffer);
            }

            if (image == null) {
                throw new RuntimeException("Failed to load texture: " + STBImage.stbi_failure_reason());
            }

            imgWidth = w.get();
            imgHeight = h.get();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load texture: " + e);
        }

        if (imgWidth != width || imgHeight != height) {
            MemoryUtil.memFree(image);
            throw new IllegalArgumentException(
                    "Texture size mismatch: expected " + width + "x" + height +
                    ", got " + imgWidth + "x" + imgHeight);
        }

        try {
            glBindTexture(GL_TEXTURE_2D_ARRAY, arrayTexId);
            glTexSubImage3D(GL_TEXTURE_2D_ARRAY, 0, 0, 0, layer, imgWidth, imgHeight, 1,
                    GL_RGBA, GL_UNSIGNED_BYTE, image);
            mipmapDirty = true;
        } finally {
            MemoryUtil.memFree(image);
        }
    }

    @Override
    public Texture getPlaceholder() {
        return placeholder;
    }

    @Override
    public void releaseLayer(int layer) {
        if (layer > 0 && layer < nextLayer) {
            freeLayers.add(layer);
        }
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
        return nextLayer - freeLayers.size();
    }

    @Override
    public void destroy() {
        glDeleteTextures(arrayTexId);
        freeLayers.clear();
    }

    private int allocateLayer() {
        if (!freeLayers.isEmpty()) {
            int layer = freeLayers.iterator().next();
            freeLayers.remove(layer);
            return layer;
        }
        if (nextLayer >= maxLayers) {
            throw new IllegalStateException("Texture layer limit exceeded: " + maxLayers);
        }
        return nextLayer++;
    }
}

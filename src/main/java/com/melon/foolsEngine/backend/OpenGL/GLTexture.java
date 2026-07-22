package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.rendering.resource.texture.LoadedImage;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.util.ImageFormatDetector;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.opengl.GL43.*;

class GLTexture implements Texture {

    private int textureId;
    private boolean uploaded = false;
    private LoadedImage image;

    @Override
    public void upload(Path texture) {
        if(uploaded) return;
        uploaded = true;

        byte[] data;
        try {
            data = Files.readAllBytes(texture);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load texture: "+e);
        }

        var format = ImageFormatDetector.detect(data);
        ByteBuffer image;
        int width, height;

        if (!ImageFormatDetector.isStbSupported(format)) {
            var result = GLImageIOLoader.load(data);
            if (result == null) {
                throw new RuntimeException(
                        "Failed to load texture: unsupported format " + format + " — " + texture);
            }
            image = result.pixels();
            width = result.width();
            height = result.height();
        } else {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                STBImage.stbi_set_flip_vertically_on_load(true);
                ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
                buffer.put(data);
                buffer.flip();

                image = STBImage.stbi_load_from_memory(buffer, w, h, channels, 4);
                MemoryUtil.memFree(buffer);
                if (image == null) {
                    var result = GLImageIOLoader.load(data);
                    if (result == null) {
                        throw new RuntimeException(
                                "Failed to load texture: " + STBImage.stbi_failure_reason());
                    }
                    image = result.pixels();
                    width = result.width();
                    height = result.height();
                } else {
                    width = w.get();
                    height = h.get();
                }
            }
        }

        final ByteBuffer capturedImage = image;
        this.image = new LoadedImage(capturedImage, width, height, () -> MemoryUtil.memFree(capturedImage));

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_RGBA,
                width,
                height,
                0,
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                image
        );
        glGenerateMipmap(GL_TEXTURE_2D);
        unbind();
    }

    @Override
    public void destroy() {
        glDeleteTextures(textureId);
    }

    @Override
    public void bind(int slot) {
        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    @Override
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    @Override
    public LoadedImage getImage() {
        return image;
    }
}
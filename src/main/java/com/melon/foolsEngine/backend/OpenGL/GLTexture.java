package com.melon.foolsEngine.backend.OpenGL;

import com.melon.foolsEngine.api.rendering.resource.LoadedImage;
import com.melon.foolsEngine.api.rendering.resource.Texture;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;


import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
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
        int width, height;
        ByteBuffer image;
        try (MemoryStack stack = MemoryStack.stackPush();
        FileInputStream fis = new FileInputStream(texture.toFile())) {

            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            //solve UV flip problem
            STBImage.stbi_set_flip_vertically_on_load(true);
            byte[] data = fis.readAllBytes();

            ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
            buffer.put(data);
            buffer.flip();

            image = STBImage.stbi_load_from_memory(buffer, w, h, channels, 4);
            MemoryUtil.memFree(buffer);
            if (image == null) {
                STBImage.stbi_failure_reason();
                throw new RuntimeException("Failed to load texture: " + STBImage.stbi_failure_reason());
            }

            width = w.get();
            height = h.get();

            this.image = new LoadedImage(image,width,height,()->{MemoryUtil.memFree(image);});
            //System.out.println(width + "x" + height);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load texture: "+e);
        }

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        //uploading texture to GPU
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        //sets MINIFICATION filtering to nearest
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        //sets MAGNIFICATION filtering to nearest
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
        //generating mipmap
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
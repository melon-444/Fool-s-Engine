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

import com.melon.foolsEngine.api.rendering.resource.texture.LoadedImage;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.core.events.EventBus;
import com.melon.foolsEngine.core.events.builtInEvents.TextureDestroyedEvent;
import com.melon.foolsEngine.core.events.builtInEvents.TextureLoadedEvent;
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
    private boolean closed;
    private LoadedImage image;

    @Override
    public void upload(Path texture) {
        requireOpen();
        if (uploaded) return;

        byte[] data;
        try {
            data = Files.readAllBytes(texture);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load texture: "+e);
        }

        var format = ImageFormatDetector.detect(data);
        ByteBuffer image;
        boolean stbAllocated = false;
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
                try {
                    buffer.put(data).flip();
                    image = STBImage.stbi_load_from_memory(buffer, w, h, channels, 4);
                } finally {
                    MemoryUtil.memFree(buffer);
                }
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
                    stbAllocated = true;
                    width = w.get();
                    height = h.get();
                }
            }
        }

        final ByteBuffer capturedImage = image;
        boolean fromStb = stbAllocated;
        LoadedImage decoded = new LoadedImage(capturedImage, width, height, () -> {
            if (fromStb) {
                STBImage.stbi_image_free(capturedImage);
            } else {
                MemoryUtil.memFree(capturedImage);
            }
        });

        try {
            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
            glGenerateMipmap(GL_TEXTURE_2D);
            unbind();
        } catch (RuntimeException | Error e) {
            decoded.free();
            if (textureId != 0) {
                glDeleteTextures(textureId);
                textureId = 0;
            }
            throw e;
        }
        this.image = decoded;
        uploaded = true;

        EventBus bus = EventBus.get("SystemBus");
        if (bus != null) bus.emit(new TextureLoadedEvent(this));
    }

    @Override
    public void destroy() {
        if (closed) {
            return;
        }
        if (!uploaded) {
            closed = true;
            return;
        }
        glDeleteTextures(textureId);
        textureId = 0;
        image.free();
        image = null;
        closed = true;
        EventBus bus = EventBus.get("SystemBus");
        if (bus != null) bus.emit(new TextureDestroyedEvent(this));
    }

    @Override
    public void bind(int slot) {
        requireOpen();
        glActiveTexture(GL_TEXTURE0 + slot);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    @Override
    public void unbind() {
        requireOpen();
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    @Override
    public LoadedImage getImage() {
        requireOpen();
        return image;
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException("Texture is closed");
        }
    }
}

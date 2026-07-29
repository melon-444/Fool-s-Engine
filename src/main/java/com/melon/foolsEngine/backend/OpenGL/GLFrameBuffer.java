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

import com.melon.foolsEngine.api.rendering.render.RenderTarget;

import static org.lwjgl.opengl.GL45.*;

class GLFrameBuffer implements RenderTarget {

    private int fbo;
    private int colorTex;
    private int depthAttachment;
    private int width;
    private int height;
    private int type;
    private int layers = 1;

    @Override
    public void init(int width, int height, int type) {
        init(width, height, type, 1);
    }

    @Override
    public void init(int width, int height, int type, int layers) {
        this.width = width;
        this.height = height;
        this.type = type;
        this.layers = layers;

        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        if (type == TARGET_DEPTH) {
            depthAttachment = glGenTextures();
            glBindTexture(GL_TEXTURE_2D_ARRAY, depthAttachment);
            glTexImage3D(GL_TEXTURE_2D_ARRAY, 0, GL_DEPTH_COMPONENT32F, width, height, layers, 0,
                    GL_DEPTH_COMPONENT, GL_FLOAT, 0L);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
            glTexParameteri(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
            float[] border = {1f, 1f, 1f, 1f};
            glTexParameterfv(GL_TEXTURE_2D_ARRAY, GL_TEXTURE_BORDER_COLOR, border);
            glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, depthAttachment, 0, 0);
            glDrawBuffer(GL_NONE);
            glReadBuffer(GL_NONE);
        }  else {
            colorTex = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, colorTex);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTex, 0);

            depthAttachment = glGenRenderbuffers();
            glBindRenderbuffer(GL_RENDERBUFFER, depthAttachment);
            glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
            glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthAttachment);
        }

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Framebuffer incomplete: 0x" + Integer.toHexString(status));
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
    }

    @Override
    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public void attachLayer(int layer) {
        glFramebufferTextureLayer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, depthAttachment, 0, layer);
    }

    @Override
    public int getLayers() {
        return layers;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getTextureId() {
        return type == TARGET_COLOR ? colorTex : depthAttachment;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public void destroy() {
        if (fbo != 0) {
            glDeleteFramebuffers(fbo);
            fbo = 0;
        }
        if (type == TARGET_COLOR) {
            if (colorTex != 0) {
                glDeleteTextures(colorTex);
                colorTex = 0;
            }
            if (depthAttachment != 0) {
                glDeleteRenderbuffers(depthAttachment);
                depthAttachment = 0;
            }
        } else {
            if (depthAttachment != 0) {
                glDeleteTextures(depthAttachment);
                depthAttachment = 0;
            }
        }
    }
}

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

    @Override
    public void init(int width, int height, int type) {
        this.width = width;
        this.height = height;
        this.type = type;

        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        if (type == TARGET_DEPTH) {
            depthAttachment = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, depthAttachment);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, width, height, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0L);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
            float[] border = {1f, 1f, 1f, 1f};
            glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, border);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthAttachment, 0);
            glDrawBuffer(GL_NONE);
            glReadBuffer(GL_NONE);
        } else {
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
            throw new RuntimeException("Framebuffer incomplete: 0x" + Integer.toHexString(status));
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
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getTextureId() {
        return type == TARGET_DEPTH ? depthAttachment : colorTex;
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
        if (type == TARGET_DEPTH) {
            if (depthAttachment != 0) {
                glDeleteTextures(depthAttachment);
                depthAttachment = 0;
            }
        } else {
            if (colorTex != 0) {
                glDeleteTextures(colorTex);
                colorTex = 0;
            }
            if (depthAttachment != 0) {
                glDeleteRenderbuffers(depthAttachment);
                depthAttachment = 0;
            }
        }
    }
}

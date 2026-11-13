package com.melon.foolsEngine.api;

import com.melon.foolsEngine.api.rendering.ui.ImGuiLayer;
import com.melon.foolsEngine.api.windows.Window;

import com.melon.foolsEngine.api.audio.AudioSystem;
import com.melon.foolsEngine.api.input.InputManager;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.api.rendering.resource.texture.TextureManager;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.WindowsManager;
import com.melon.foolsEngine.backend.JavaSound.JavaSoundAudioSystem;
import com.melon.foolsEngine.backend.OpenAL.OpenALAudioSystem;
import com.melon.foolsEngine.backend.OpenGL.GLInternalFactory;

import java.util.Objects;
import java.util.function.Supplier;

/** Selects one graphics backend and optionally one audio backend for an engine. */
public final class APIBuilder {
    private Supplier<? extends APIFactory> graphics;
    private Supplier<? extends AudioSystem> audio;

    private APIBuilder() {}

    public static APIBuilder builder() {
        return new APIBuilder();
    }

    public APIBuilder OpenGL() {
        return selectGraphics(GLInternalFactory::create);
    }

    /** Selects Vulkan. Building currently fails because the backend is still a stub. */
    public APIBuilder Vulkan() {
        return selectGraphics(() -> {
            throw new UnsupportedOperationException("The Vulkan backend is not implemented");
        });
    }

    /** Uses this factory for graphics, windowing and input. Select audio separately. */
    public APIBuilder customGraphics(APIFactory factory) {
        Objects.requireNonNull(factory, "factory");
        return selectGraphics(() -> factory);
    }

    public APIBuilder JavaSound() {
        return selectAudio(JavaSoundAudioSystem::new);
    }

    public APIBuilder OpenAL() {
        return selectAudio(OpenALAudioSystem::new);
    }

    /** The provider must return a new caller-owned audio system on each invocation. */
    public APIBuilder customAudio(Supplier<? extends AudioSystem> provider) {
        return selectAudio(Objects.requireNonNull(provider, "provider"));
    }

    private APIBuilder selectGraphics(Supplier<? extends APIFactory> provider) {
        if (graphics != null) {
            throw new IllegalStateException("A graphics backend has already been selected");
        }
        graphics = provider;
        return this;
    }

    private APIBuilder selectAudio(Supplier<? extends AudioSystem> provider) {
        if (audio != null) {
            throw new IllegalStateException("An audio backend has already been selected");
        }
        audio = provider;
        return this;
    }

    /** Fixes the backend selection without opening a window or audio device. */
    public APIFactory create() {
        if (graphics == null) {
            throw new IllegalStateException("Select a graphics backend before creating the API factory");
        }
        return new ConfiguredFactory(graphics.get(), audio);
    }

    private record ConfiguredFactory(APIFactory graphics, Supplier<? extends AudioSystem> audio)
            implements APIFactory {
        @Override
        public ImGuiLayer createImGuiLayer(Window window) {
            return graphics.createImGuiLayer(window);
        }

        @Override public WindowsManager getWindowsManager() { return graphics.getWindowsManager(); }
        @Override public RenderFrame getRenderFrame() { return graphics.getRenderFrame(); }
        @Override public ShaderProgram getShaderProgram() { return graphics.getShaderProgram(); }
        @Override public Texture getTexture() { return graphics.getTexture(); }
        @Override public Mesh getMesh() { return graphics.getMesh(); }
        @Override public String implementVendor() { return graphics.implementVendor(); }

        @Override
        public RenderTarget createRenderTarget(int width, int height, int type) {
            return graphics.createRenderTarget(width, height, type);
        }

        @Override
        public RenderTarget createRenderTarget(int width, int height, int type, int layers) {
            return graphics.createRenderTarget(width, height, type, layers);
        }

        @Override
        public <E> InputManager createInputManager(E env) {
            return graphics.createInputManager(env);
        }

        @Override
        public TextureManager createTextureManager(int width, int height, int maxLayers) {
            return graphics.createTextureManager(width, height, maxLayers);
        }

        @Override
        public boolean supportsAudio() {
            return audio != null;
        }

        @Override
        public AudioSystem createAudioSystem() {
            if (!supportsAudio()) {
                return APIFactory.super.createAudioSystem();
            }
            return Objects.requireNonNull(audio.get(), "Audio provider returned null");
        }
    }
}

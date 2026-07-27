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

package com.melon.foolsEngine.core;

import com.melon.foolsEngine.api.rendering.render.GraphicsContext;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.resource.shadow.ShadowPassContext;
import com.melon.foolsEngine.api.rendering.shader.ShaderPass;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.Window;
import com.melon.foolsEngine.core.ECS.basicComponents.*;
import com.melon.foolsEngine.core.ECS.entity.EntityFactory;
import com.melon.foolsEngine.core.ECS.system.*;
import com.melon.foolsEngine.core.annotation.EventBus;
import com.melon.foolsEngine.core.world.*;
import com.melon.foolsEngine.util.logger.Logger;

@EventBus(id="SystemBus")
public class FoolsEngine {
    public final EntityManager entityManager;
    public final EntityFactory entityFactory;
    public final ComponentManager componentManager;
    public final SystemManager systemManager;
    public final EntityFactory factory;
    public final RenderFrame frame;
    public final ServiceFactory serviceFactory;
    public final SystemScheduler systemScheduler;
    public final boolean isServer;

    public final Logger LOGGER = new Logger("SYSTEM");

    public final int MAX_ENTITIES;
    public final int MAX_COMPONENTS;

    public final boolean REVERSE_Z = true;

    public int width;
    public int height;
    public float aspect;

    public float FOV;
    public float Z_NEAR = 0.01f;
    public float Z_FAR = 1E10f;

    public Window mainWindow;

    /**
     * Optional, configurable templates for commonly used rendering passes.
     *
     * <p>These methods only create builders. They do not register systems,
     * create ECS entities, or submit anything to a {@code RenderScene}.</p>
     */
    public static final class StandardPasses {

        private StandardPasses() {
        }

        /** Main CORE pass using each command's material shader and parameters. */
        public static ShaderPass.Builder core() {
            return ShaderPass.core()
                    .colorOps(
                            ShaderPass.LoadOp.CLEAR,
                            ShaderPass.StoreOp.STORE)
                    .depthOps(
                            ShaderPass.LoadOp.CLEAR,
                            ShaderPass.StoreOp.STORE);
        }

        /** CORE pass using one pass shader and each command's material parameters. */
        public static ShaderPass.Builder core(ShaderProgram shader) {
            return ShaderPass.core(shader)
                    .colorOps(
                            ShaderPass.LoadOp.CLEAR,
                            ShaderPass.StoreOp.STORE)
                    .depthOps(
                            ShaderPass.LoadOp.CLEAR,
                            ShaderPass.StoreOp.STORE);
        }

        /** Fullscreen post-effect that preserves its color result. */
        public static ShaderPass.Builder postEffect(ShaderProgram shader) {
            return ShaderPass.postEffect(shader)
                    .colorOps(
                            ShaderPass.LoadOp.LOAD,
                            ShaderPass.StoreOp.STORE)
                    .depthOps(
                            ShaderPass.LoadOp.DONT_CARE,
                            ShaderPass.StoreOp.DONT_CARE);
        }

        /**
         * Depth-only shadow pass for a context prepared by a ShadowManager.
         * The caller remains responsible for deciding when and for which lights
         * this pass is created and submitted.
         */
        public static ShaderPass.Builder shadow(ShadowPassContext context) {
            if (context == null) {
                throw new NullPointerException("context");
            }
            return ShaderPass.core()
                    .output(context.target())
                    .camera(context.shadowCamera())
                    .overrideMaterial(context.depthMaterial())
                    .arrayLayer(context.layer())
                    .colorOps(
                            ShaderPass.LoadOp.DONT_CARE,
                            ShaderPass.StoreOp.DONT_CARE)
                    .depthOps(
                            ShaderPass.LoadOp.CLEAR,
                            ShaderPass.StoreOp.STORE)
                    .clearDepth(0.0);
        }
    }

    FoolsEngine(int maxEntities, int maxComponents, int windowWidth, int windowHeight, boolean isServer) {
        this.isServer = isServer;
        LOGGER.info("Booting FoolsEngine | entities=%d components=%d mode=%s", maxEntities, maxComponents,
                isServer ? "SERVER" : "CLIENT");
        this.serviceFactory = new ServiceFactory();
        this.MAX_ENTITIES = maxEntities;
        this.MAX_COMPONENTS = maxComponents;
        this.entityManager = new EntityManager(this);
        this.componentManager = new ComponentManager(this);
        this.systemManager = new SystemManager(this);
        this.factory = new EntityFactory(this);
        this.entityFactory = new EntityFactory(this);
        this.width = windowWidth;
        this.height = windowHeight;
        LOGGER.debug("Managers initialized");

        if (!isServer) {
            LOGGER.info("Initializing client subsystems");
            this.frame = serviceFactory.getRenderFrame();
            mainWindow = serviceFactory.getWindowsManager().createWindow();
            mainWindow.setSize(windowWidth, windowHeight);
            LOGGER.debug("Window created | %dx%d", windowWidth, windowHeight);
            systemManager.registerSystem(LightEnvCollector.class);
            systemManager.registerSystem(TextureManagerCollector.class);
            systemManager.registerSystem(CameraCollector.class);
            systemManager.registerSystem(LightCollector.class);
            systemManager.registerSystem(RenderableCollector.class);
            systemManager.registerSystem(RenderPassCollector.class);
            systemManager.registerSystem(MaterialCollector.class);
            LOGGER.debug("Systems registered");
            frame.init();
            LOGGER.debug("RenderFrame initialized");
            this.systemScheduler = new SystemScheduler(frame, (GraphicsContext) mainWindow,systemManager);
            LOGGER.debug("SystemScheduler created");
        } else {
            this.frame = null;
            this.systemScheduler = new SystemScheduler(systemManager);
            LOGGER.debug("Headless SystemScheduler created");
        }
        LOGGER.info("Boot complete");
    }

    /**
     * Update setting in files, currently they are merely useless.
     */
    public void updateSettings() {
        aspect = (float) width / (float) height;
        FOV = 40.0f;
    }

    /**
     * Loads the built-in Phong + shadow mapping shaders from classpath resources.
     * Works in both IDE projects and fat JARs.
     *
     * @return a two-element array: [0] = main shader (Phong), [1] = depth shader (shadow map)
     */
    public ShaderProgram[] loadBuiltinShaders() {
        ShaderProgram mainShader = serviceFactory.getShaderProgram();
        mainShader.load("/shader/main/main_vsh.glsl", "/shader/main/main_fsh.glsl");
        LOGGER.info("Built-in main shader loaded");

        ShaderProgram depthShader = serviceFactory.getShaderProgram();
        depthShader.load("/shader/depth/depth_vsh.glsl", "/shader/depth/depth_fsh.glsl");
        LOGGER.info("Built-in depth shader loaded");

        return new ShaderProgram[]{ mainShader, depthShader };
    }
}
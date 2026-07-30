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
package com.melon.foolsEngineTest;

import com.melon.foolsEngine.api.input.*;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.pipeline.ShaderPass;
import com.melon.foolsEngine.api.rendering.resource.*;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.api.rendering.resource.texture.TextureManager;
import com.melon.foolsEngine.api.rendering.shader.BuiltinShaders;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.Window;
import com.melon.foolsEngine.api.windows.WindowsManager;
import com.melon.foolsEngine.core.ECS.basicComponents.*;
import com.melon.foolsEngine.core.ECS.system.ServerSystem;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.core.ECS.system.ShadowPassCollector;
import com.melon.foolsEngine.core.bootstrap.EngineBoot;
import com.melon.foolsEngine.core.world.SystemScheduler;
import com.melon.foolsEngine.util.*;
import com.melon.foolsEngine.util.imgui.ImGuiContext;
import com.melon.foolsEngine.util.imgui.ImGuiDebugOverlay;
import com.melon.foolsEngine.util.imgui.ImGuiRenderer;
import com.melon.foolsEngine.util.logger.LogLevel;
import com.melon.foolsEngine.util.logger.Logger;
import org.joml.*;
import org.joml.Math;

import java.nio.file.Path;

public class TesECSRenderFlow {
    static FoolsEngine foolsEngine;
    private static final int SHADOW_MAP_SIZE = 8192;
    private static final int MAX_SHADOW_LAYERS = 32;
    private static final float SPOT_SHADOW_NEAR = 0.1f;

    private static final Logger TESTLOGGER = new Logger();

    private static final class CameraMovementSystem extends ServerSystem<Void> {

        private final Window win = INSTANCE.mainWindow;
        private final InputManager input;
        private final Action moveForward   = () -> SignalType.BUTTON;
        private final Action moveBackward  = () -> SignalType.BUTTON;
        private final Action moveLeft      = () -> SignalType.BUTTON;
        private final Action moveRight     = () -> SignalType.BUTTON;
        private final Action moveUp        = () -> SignalType.BUTTON;
        private final Action moveDown      = () -> SignalType.BUTTON;
        private final Action lookDelta     = () -> SignalType.AXIS_2DDel;

        private final SparseSet<TransformComponent> camTrans;
        private final SparseSet<CameraComponent> cameras;

        private float yaw;
        private float pitch;

        private static final float MOVE_SPEED = 5.0f;
        private static final float LOOK_SENSITIVITY = 1.0f;

        private final Vector3f worldUp   = new Vector3f(0, 1, 0);
        private final Vector3f lookDir   = new Vector3f();
        private final Vector3f right     = new Vector3f();
        private final Vector3f tmpMove   = new Vector3f();

        {
            requiredComponents.add(CameraComponent.class);
            requiredComponents.add(TransformComponent.class);
        }

        CameraMovementSystem(FoolsEngine engine) {
            super(engine, null);
            input = getService(InputManager.class);
            cameras = getSparseSet(CameraComponent.class);
            camTrans = getSparseSet(TransformComponent.class);
            input.bind(input.getKeyboard(), FoolsEngineKeyCode.W,      moveForward);
            input.bind(input.getKeyboard(), FoolsEngineKeyCode.S,      moveBackward);
            input.bind(input.getKeyboard(), FoolsEngineKeyCode.A,      moveLeft);
            input.bind(input.getKeyboard(), FoolsEngineKeyCode.D,      moveRight);
            input.bind(input.getKeyboard(), FoolsEngineKeyCode.SPACE,       moveUp);
            input.bind(input.getKeyboard(), FoolsEngineKeyCode.LEFT_SHIFT,  moveDown);
            input.bind(input.getMouse(),    FoolsEngineKeyCode.CURSOR, lookDelta);
        }

        @Override
        public void update(float dt, Void unused) {
            TransformComponent ctx = null;
            for(int entity:entities){
                CameraComponent camera = cameras.get(entity);
                TransformComponent trans = camTrans.get(entity);
                if(camera.isMainCam) {
                    ctx = trans;
                    break;
                }
            }
            if(ctx == null) throw new NullPointerException("TransformComponent is null.");

            Vector2f mouseDelta = win.getCursorMode() == CursorMode.DISABLED
                    ? input.getActionAxis2DDelta(lookDelta)
                    : new Vector2f(0.0f);
            boolean transformChanged = false;
            if (mouseDelta.lengthSquared() > 0.0f) {
                yaw   -= mouseDelta.x * LOOK_SENSITIVITY;
                pitch -= mouseDelta.y * LOOK_SENSITIVITY;
                pitch = Math.min(89.5f, Math.max(-89.5f, pitch));

                ctx.getRotation().identity();
                ctx.getRotation().rotateY(Math.toRadians(yaw));
                ctx.getRotation().rotateX(Math.toRadians(pitch));
                transformChanged = true;
            }

            lookDir.set(
                    -(float) Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)),
                    Math.sin(Math.toRadians(pitch)),
                    -(float) Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))
            ).normalize();

            right.set(lookDir).cross(worldUp).normalize();

            tmpMove.set(0);
            if (input.isActionDown(moveForward))  tmpMove.add(lookDir);
            if (input.isActionDown(moveBackward)) tmpMove.sub(lookDir);
            if (input.isActionDown(moveRight))    tmpMove.add(right);
            if (input.isActionDown(moveLeft))     tmpMove.sub(right);
            if (input.isActionDown(moveUp))       tmpMove.add(worldUp);
            if (input.isActionDown(moveDown))     tmpMove.sub(worldUp);

            if (tmpMove.lengthSquared() > 1e-12f) {
                tmpMove.normalize().mul(MOVE_SPEED * dt);
                ctx.getPosition().add(tmpMove);
                transformChanged = true;
            }
            if (transformChanged) {
                ctx.markDirty();
            }
        }
    }

    public static void main(String[] args) {
        Thread engineThread = new Thread(() -> {run(args);});
        engineThread.setName("EngineMain");
        engineThread.start();
    }

    public static void run(String[] args) {
        foolsEngine = EngineBoot.create(20000000, 100, 2560, 1600, false, LogLevel.DEBUG);

        WindowsManager manager = foolsEngine.serviceFactory.getWindowsManager();
        Window win = foolsEngine.mainWindow;
        win.setTitle("TesECSRenderFlow - ECS + SystemScheduler + Shadows");
        win.setSize(2048, 1536);

        Mesh dragonMesh = foolsEngine.serviceFactory.getMesh();
        dragonMesh.upload(ObjLoader.loadMesh(Path.of("src/test/resources/shaders/model/dragon.obj")));

        BuiltinShaders builtinShaders =
                BuiltinShaders.load(foolsEngine.serviceFactory);
        ShaderProgram shader = builtinShaders.main();
        ShaderProgram depthShader = builtinShaders.shadowDepth();
        Material depthMaterial = new Material(depthShader);

        Material material = new Material(shader);
        Texture texture = foolsEngine.serviceFactory.getTexture();
        texture.upload(Path.of("src/test/resources/textures/test2.png"));
        material.set("textureSampler", texture);

        TextureManager textureManager = foolsEngine.serviceFactory.createTextureManager(256, 256, 64);
        Texture arrayTexture = textureManager.upload(Path.of("src/test/resources/textures/test2.png"));
        Material arrayMaterial = new Material(shader);
        arrayMaterial.set("textureSampler", arrayTexture);

        Vector3f origin = new Vector3f(0, 0, 5);
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                for(int k=0;k<10;k++){
                    Material mat = (i % 2 == 0) ? arrayMaterial : material;
                    Vector3f pos = new Vector3f(origin).add(3 * i, 5*k, 2 * j);
                    foolsEngine.entityFactory.createModelEntity(
                            dragonMesh, mat, pos, new Quaternionf(), new Vector3f(0.1f, 0.1f, 0.1f));
                }
            }
        }

        java.util.List<Integer> lightEntities = new java.util.ArrayList<>();

        Vector3f cameraPos = new Vector3f(0, 0, 12);
        TransformComponent cameraTransform = foolsEngine.entityFactory.createCamera(cameraPos);

        win.show();
        RenderFrame frame = foolsEngine.frame;
        foolsEngine.entityFactory.createShaderPass(
                0,
                ShaderPass.core()
                        .colorOps(
                                ShaderPass.LoadOp.CLEAR,
                                ShaderPass.StoreOp.STORE)
                        .depthOps(
                                ShaderPass.LoadOp.CLEAR,
                                ShaderPass.StoreOp.STORE)
                        .build());


        ImGuiContext imGuiContext = new ImGuiContext();
        imGuiContext.init(win.getID(), "#version 330");
        ImGuiRenderer imGuiRenderer = new ImGuiRenderer(imGuiContext);
        ImGuiDebugOverlay debugOverlay = new ImGuiDebugOverlay();

        foolsEngine.systemManager.registerSystem(ShadowPassCollector.class);
        int lightEnvEntity = foolsEngine.entityFactory.createLightEnvironment();
        LightEnvironment lightEnv = ((LightEnvComponent)foolsEngine.componentManager.getComponentMap().get(LightEnvComponent.class).get(lightEnvEntity)).env;
        lightEnv.setAmbient(0.08f, 0.08f, 0.08f);
        lightEnv.setShadowMapSize(SHADOW_MAP_SIZE);

        RenderTarget shadowArray = foolsEngine.serviceFactory.createRenderTarget(
                SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, RenderTarget.TARGET_DEPTH, MAX_SHADOW_LAYERS);
        lightEnv.enableShadows(shadowArray, depthMaterial, MAX_SHADOW_LAYERS);

        int textureMgrEntity = foolsEngine.entityManager.createEntity();
        foolsEngine.entityManager.bindComponent(textureMgrEntity,
                new TextureManagerComponent(textureManager));

        SystemScheduler scheduler = foolsEngine.systemScheduler;
        RenderScene scene = scheduler.getScene();
        scene.setBackGroundColor(lightEnv.getAmbient().x, lightEnv.getAmbient().y, lightEnv.getAmbient().z, 1.0f);

        InputManager input = foolsEngine.serviceFactory.createInputManager(win);
        win.setCursorMode(CursorMode.DISABLED);

        Action spawnDirLight = () -> SignalType.BUTTON;
        Action spawnPointLight = () -> SignalType.BUTTON;
        Action spawnSpotLight = () -> SignalType.BUTTON;
        Action clearLights = () -> SignalType.BUTTON;
        Action ambientUp = () -> SignalType.BUTTON;
        Action ambientDown = () -> SignalType.BUTTON;
        Action exit = () -> SignalType.BUTTON;
        Action spawnShadowDirLight = () -> SignalType.BUTTON;
        Action spawnShadowSpotLight = () -> SignalType.BUTTON;
        Action switchMouseMode = () -> SignalType.BUTTON;
        Action switchDebugWindow = () -> SignalType.BUTTON;
        Action switchFullscreen = () -> SignalType.BUTTON;

        input.bind(input.getKeyboard(), FoolsEngineKeyCode.J, ambientUp);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.K, ambientDown);

        input.bind(input.getKeyboard(), FoolsEngineKeyCode.P, spawnDirLight);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.O, spawnPointLight);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.I, spawnSpotLight);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.L, clearLights);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.COMMA, spawnShadowDirLight);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.N, spawnShadowSpotLight);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.ESC, exit);

        input.bind(input.getKeyboard(), FoolsEngineKeyCode.C, switchDebugWindow);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.F11, switchFullscreen);

        input.bind(input.getMouse(), FoolsEngineKeyCode.MOUSE_RIGHT, switchMouseMode);

        foolsEngine.registerService(InputManager.class, input);
        foolsEngine.systemManager.registerSystem(CameraMovementSystem.class);

        boolean renderDebug = false;

        java.util.Random rng = new java.util.Random();
        long lastTime = System.nanoTime();

        while (!win.shouldClose()) {
            long currentTime = System.nanoTime();
            float deltaTime = (currentTime - lastTime) / 1e9f;
            lastTime = currentTime;

            long updateStartNs = System.nanoTime();
            scheduler.update();
            long updateElapsedNs = System.nanoTime() - updateStartNs;
            float updateTimeMs = updateElapsedNs / 1e6f;
            foolsEngine.LOGGER.trace("renderStart:%d, renderTimeMs:%f",updateStartNs, updateTimeMs);
            input.updateFromPolledInputs();

            if (renderDebug) {
                scheduler.additionalRenderTask(() -> {
                    imGuiRenderer.beginFrame();
                    Vector3f cp = cameraTransform.getPosition();
                    debugOverlay.render(scene, deltaTime, updateTimeMs,
                            cp, 0, 0, frame.getDrawCallCount());
                    imGuiRenderer.endFrame();
                });
            } else {
                scheduler.additionalRenderTask(() -> {});
            }

            Vector3f lookDir = new Vector3f(cameraTransform.getPosition()).negate().normalize();

            if (input.isActionDown(exit)) {
                break;
            }

            if (input.isActionPressed(spawnDirLight)) {
                Vector3f color = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
                lightEntities.add(foolsEngine.entityFactory.createLightEntity(
                        new LightComponent(color, new Vector3f(lookDir))));
            }
            if (input.isActionPressed(spawnPointLight)) {
                Vector3f color = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
                lightEntities.add(foolsEngine.entityFactory.createLightEntity(
                        new LightComponent(color, new Vector3f(lookDir),
                                new Vector3f(cameraTransform.getPosition()))));
            }
            if (input.isActionPressed(spawnSpotLight)) {
                Vector3f color = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
                lightEntities.add(foolsEngine.entityFactory.createLightEntity(
                        new LightComponent(color, new Vector3f(lookDir),
                                new Vector3f(cameraTransform.getPosition()), 10f, 10f)));
            }
            if (input.isActionPressed(clearLights)) {
                for (int eid : lightEntities) {
                    foolsEngine.entityManager.destroyEntity(eid);
                }
                lightEntities.clear();
                lightEnv.clear();
            }
            if (input.isActionPressed(ambientUp)) {
                lightEnv.getAmbient().mul(1.1f);
                scene.setBackGroundColor(lightEnv.getAmbient().x, lightEnv.getAmbient().y, lightEnv.getAmbient().z, 1.0f);
            }
            if (input.isActionPressed(ambientDown)) {
                lightEnv.getAmbient().mul(0.9f);
                scene.setBackGroundColor(lightEnv.getAmbient().x, lightEnv.getAmbient().y, lightEnv.getAmbient().z, 1.0f);
            }
            if (input.isActionPressed(spawnShadowDirLight)) {
                Vector3f color = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
                LightComponent LightComp =
                        new LightComponent(color, new Vector3f(lookDir));
                LightComp.castsShadow = true;
                lightEntities.add(foolsEngine.entityFactory.createLightEntity(LightComp));
            }
            if (input.isActionPressed(spawnShadowSpotLight)) {
                Vector3f color = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
                LightComponent LightComp =
                        new LightComponent(color, new Vector3f(lookDir),
                                new Vector3f(cameraTransform.getPosition()), 10f, 10f);
                LightComp.castsShadow = true;
                LightComp.shadowNear = SPOT_SHADOW_NEAR;
                LightComp.intensity = 2.0f;
                lightEntities.add(foolsEngine.entityFactory.createLightEntity(LightComp));
            }
            if (input.isActionPressed(switchMouseMode)) {
                if (win.getCursorMode() == CursorMode.DISABLED)
                    win.setCursorMode(CursorMode.NORMAL);
                else
                    win.setCursorMode(CursorMode.DISABLED);
            }

            if (input.isActionPressed(switchDebugWindow)) {
                renderDebug = !renderDebug;
            }

            if (input.isActionPressed(switchFullscreen)) {
                win.setFullscreen(!win.isFullscreen());
                input.getMouse().flushDeltas();
            }

            input.clearPolledInputs();
        }

        shader.destroy();
        depthShader.destroy();
        lightEnv.destroy();
        manager.destroyWindow(win, true);
    }
}

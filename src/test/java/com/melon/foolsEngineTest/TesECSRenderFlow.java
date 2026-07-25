package com.melon.foolsEngineTest;

import com.melon.foolsEngine.api.input.*;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.render.ShaderPass;
import com.melon.foolsEngine.api.rendering.resource.*;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.api.rendering.resource.texture.TextureManager;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.Window;
import com.melon.foolsEngine.api.windows.WindowsManager;
import com.melon.foolsEngine.core.ECS.basicComponents.LightComp;
import com.melon.foolsEngine.core.ECS.basicComponents.LightEnvComponent;
import com.melon.foolsEngine.core.ECS.basicComponents.TextureManagerComponent;
import com.melon.foolsEngine.core.ECS.basicComponents.TransformComp;
import com.melon.foolsEngine.core.EngineBoot;
import com.melon.foolsEngine.core.FoolsEngine;
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
    static FoolsEngine foolsEngine = EngineBoot.create(20000000, 100, 2560, 1600, false, LogLevel.DEBUG);
    private static final int SHADOW_MAP_SIZE = 8192;
    private static final int MAX_SHADOW_LAYERS = 16;
    private static final float SPOT_SHADOW_NEAR = 0.1f;

    private static final Logger TESTLOGGER = new Logger();

    public static void main(String[] args) {
        WindowsManager manager = foolsEngine.serviceFactory.getWindowsManager();
        Window win = foolsEngine.mainWindow;
        win.setTitle("TesECSRenderFlow - ECS + SystemScheduler + Shadows");
        win.setSize(2048, 1536);

        Mesh dragonMesh = foolsEngine.serviceFactory.getMesh();
        dragonMesh.upload(ObjLoader.loadMesh(Path.of("src/test/resources/shaders/model/dragon.obj")));

        ShaderProgram[] builtinShaders = foolsEngine.loadBuiltinShaders();
        ShaderProgram shader = builtinShaders[0];
        ShaderProgram depthShader = builtinShaders[1];
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
                for(int k=0;k<1;k++){
                    Material mat = (i % 2 == 0) ? arrayMaterial : material;
                    Vector3f pos = new Vector3f(origin).add(3 * i, 5*k, 2 * j);
                    foolsEngine.entityFactory.createModelEntity(
                            dragonMesh, mat, pos, new Quaternionf(), new Vector3f(0.1f, 0.1f, 0.1f));
                }
            }
        }

        java.util.List<Integer> lightEntities = new java.util.ArrayList<>();

        Vector3f cameraPos = new Vector3f(0, 0, 12);
        TransformComp cameraTransform = foolsEngine.entityFactory.createCamera(cameraPos);

        win.show();
        RenderFrame frame = foolsEngine.frame;
        foolsEngine.entityFactory.createShaderPass(0, ShaderPass.color(shader));

        ImGuiContext imGuiContext = new ImGuiContext();
        imGuiContext.init(win.getID(), "#version 330");
        ImGuiRenderer imGuiRenderer = new ImGuiRenderer(imGuiContext);
        ImGuiDebugOverlay debugOverlay = new ImGuiDebugOverlay();



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

        Action moveForward = () -> SignalType.BUTTON;
        Action moveBackward = () -> SignalType.BUTTON;
        Action moveLeft = () -> SignalType.BUTTON;
        Action moveRight = () -> SignalType.BUTTON;
        Action moveUp = () -> SignalType.BUTTON;
        Action moveDown = () -> SignalType.BUTTON;
        Action lookDelta = () -> SignalType.AXIS_2DDel;
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

        input.bind(input.getKeyboard(), FoolsEngineKeyCode.W, moveForward);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.S, moveBackward);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.A, moveLeft);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.D, moveRight);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.SPACE, moveUp);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.LEFT_SHIFT, moveDown);

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

        input.bind(input.getMouse(), FoolsEngineKeyCode.CURSOR, lookDelta);
        input.bind(input.getMouse(), FoolsEngineKeyCode.MOUSE_RIGHT, switchMouseMode);

        float moveSpeed = 5.0f;
            float lookSensitivity = 1.0f;
            float yaw = 0;
            float pitch = 0;

            Vector3f worldUp = new Vector3f(0, 1, 0);

            boolean renderDebug = false;

        java.util.Random rng = new java.util.Random();

        long lastTime = System.nanoTime();

        while (!win.shouldClose()) {
            long currentTime = System.nanoTime();
            float deltaTime = (currentTime - lastTime) / 1e9f;
            lastTime = currentTime;


            float renderStart = System.nanoTime();
            scheduler.update();
            float renderTimeMs = (System.nanoTime() - renderStart) / 1e6f;

            if (renderDebug) {
                float finalYaw = yaw;
                float finalPitch = pitch;
                scheduler.additionalRenderTask(() -> {
                        imGuiRenderer.beginFrame();
                        debugOverlay.render(scene, deltaTime, renderTimeMs,
                                cameraPos, finalYaw, finalPitch, frame.getDrawCallCount());
                        imGuiRenderer.endFrame();
                });
            }else{
                scheduler.additionalRenderTask(() -> {});
            }
            input.beginFrame();

            Vector3f lookDir = new Vector3f(
                    -(float)Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)),
                    Math.sin(Math.toRadians(pitch)),
                    -(float)Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))
            ).normalize();
            Vector3f right = new Vector3f(lookDir).cross(worldUp).normalize();

            if (input.isActionDown(moveForward)) {
                cameraPos.add(new Vector3f(lookDir).mul(moveSpeed * 0.016f));
            }
            if (input.isActionDown(moveBackward)) {
                cameraPos.sub(new Vector3f(lookDir).mul(moveSpeed * 0.016f));
            }
            if (input.isActionDown(moveRight)) {
                cameraPos.add(new Vector3f(right).mul(moveSpeed * 0.016f));
            }
            if (input.isActionDown(moveLeft)) {
                cameraPos.sub(new Vector3f(right).mul(moveSpeed * 0.016f));
            }
            if (input.isActionDown(moveUp)) {
                cameraPos.add(new Vector3f(worldUp).mul(moveSpeed * 0.016f));
            }
            if (input.isActionDown(moveDown)) {
                cameraPos.sub(new Vector3f(worldUp).mul(moveSpeed * 0.016f));
            }
            if (input.isActionDown(exit)) {
                break;
            }

            Vector2f mouseDelta = win.getCursorMode() == CursorMode.DISABLED ? input.getActionAxis2DDelta(lookDelta) : new Vector2f(0.0f);
            yaw -= mouseDelta.x * lookSensitivity;
            pitch -= mouseDelta.y * lookSensitivity;
            pitch = Math.min(89.5f, Math.max(-89.5f, pitch));


            cameraTransform.getRotation().identity();
            cameraTransform.getRotation().rotateY(Math.toRadians(yaw));
            cameraTransform.getRotation().rotateX(Math.toRadians(pitch));
            cameraTransform.position(cameraPos);// mark dirty at the same time

            if (input.isActionPressed(spawnDirLight)) {
                Vector3f color = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
                lightEntities.add(foolsEngine.entityFactory.createLightEntity(
                        new LightComp(color, new Vector3f(lookDir))));
            }
            if (input.isActionPressed(spawnPointLight)) {
                Vector3f color = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
                lightEntities.add(foolsEngine.entityFactory.createLightEntity(
                        new LightComp(color, new Vector3f(lookDir), new Vector3f(cameraPos))));
            }
            if (input.isActionPressed(spawnSpotLight)) {
                Vector3f color = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
                lightEntities.add(foolsEngine.entityFactory.createLightEntity(
                        new LightComp(color, new Vector3f(lookDir), new Vector3f(cameraPos), 10f, 10f)));
            }
            if (input.isActionPressed(clearLights)) {
                TESTLOGGER.info("ActualLightCounts: %d",scene.getLighting().getLights().size());
                for (int eid : lightEntities) {
                    TESTLOGGER.info("clearLight: %d",eid);
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
                LightComp LightComp =
                        new LightComp(color, new Vector3f(lookDir));
                LightComp.castsShadow = true;
                lightEntities.add(foolsEngine.entityFactory.createLightEntity(LightComp));
            }
            if (input.isActionPressed(spawnShadowSpotLight)) {
                Vector3f color = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
                LightComp LightComp =
                        new LightComp(color, new Vector3f(lookDir),
                                new Vector3f(cameraPos), 10f, 10f);
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

            if(input.isActionPressed(switchFullscreen)) {
                win.setFullscreen(!win.isFullscreen());
            }

            input.endFrame();
        }

        shader.destroy();
        depthShader.destroy();
        lightEnv.destroy();
        manager.destroyWindow(win, true);
    }
}

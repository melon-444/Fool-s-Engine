package com.melon.foolsEngineTest;

import com.melon.foolsEngine.api.input.Action;
import com.melon.foolsEngine.api.input.FoolsEngineKeyCode;
import com.melon.foolsEngine.api.input.InputManager;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import com.melon.foolsEngine.api.rendering.resource.*;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.Window;
import com.melon.foolsEngine.api.windows.WindowsManager;
import com.melon.foolsEngine.backend.OpenGL.GLFWKeyBoard;
import com.melon.foolsEngine.backend.OpenGL.GLFWMouse;
import com.melon.foolsEngine.core.ECS.basicComponents.Transform;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.*;
import com.melon.foolsEngine.util.imgui.ImGuiContext;
import com.melon.foolsEngine.util.imgui.ImGuiDebugOverlay;
import com.melon.foolsEngine.util.imgui.ImGuiRenderer;
import org.joml.*;
import org.joml.Math;

import java.nio.file.Path;

public class TestLightBackend {
    static FoolsEngine foolsEngine = FoolsEngine.create(1000, 100, 800, 600);
    private static final int SHADOW_MAP_SIZE = 8192;
    private static final int MAX_SHADOW_LAYERS = 16;
    private static final float SPOT_SHADOW_NEAR = 0.1f;

    public static void main(String[] args) {
        WindowsManager manager = foolsEngine.serviceFactory.getWindowsManager();
        Window win = foolsEngine.mainWindow;
        win.setTitle("Test Light Backend - P:Dir  O:Point  I:Spot  ,:ShadowDir  N:ShadowSpot  L:Clear J:AmbientUp K: AmbientDown ESC:exit");
        win.setSize(2048, 1536);

        Mesh dragonMesh = foolsEngine.serviceFactory.getMesh();
        dragonMesh.upload(ObjLoader.loadMesh(Path.of("src/test/resources/shaders/model/dragon.obj")));

        ShaderProgram shader = foolsEngine.serviceFactory.getShaderProgram();
        shader.load(Path.of("src/main/resources/shader/vsh/main_vsh.glsl"), Path.of("src/main/resources/shader/fsh/main_fsh.glsl"));
        Material material = new Material(shader);
        Texture texture = foolsEngine.serviceFactory.getTexture();
        texture.upload(Path.of("src/test/resources/textures/test2.png"));
        material.set("textureSampler", texture);

        ShaderProgram depthShader = foolsEngine.serviceFactory.getShaderProgram();
        depthShader.load(Path.of("src/main/resources/shader/vsh/depth_vsh.glsl"), Path.of("src/main/resources/shader/fsh/depth_fsh.glsl"));
        Material depthMaterial = new Material(depthShader);

        Transform dragonTransform1 = new Transform(new Vector3f(0, 0, 5), new Quaternionf(), new Vector3f(0.1f, 0.1f, 0.1f));
        Transform dragonTransform2 = new Transform(new Vector3f(0, 0, -5), new Quaternionf(), new Vector3f(0.1f, 0.1f, 0.1f));

        PerspectiveProjection proj = new PerspectiveProjection(foolsEngine.FOV, foolsEngine.aspect, foolsEngine.Z_NEAR);
        Vector3f cameraPos = new Vector3f(0, 0, -12);
        Vector3f cameraTarget = new Vector3f(0, 0, 0);
        Vector3f worldUp = new Vector3f(0, 1, 0);
        Camera camera = new Camera(
                new Matrix4f().lookAt(cameraPos, cameraTarget, worldUp),
                proj.get(new Matrix4f())
        );

        win.show();
        RenderFrame frame = foolsEngine.frame;
        frame.init();

        ImGuiContext imGuiContext = new ImGuiContext();
        imGuiContext.init(win.getID(), "#version 330");
        ImGuiRenderer imGuiRenderer = new ImGuiRenderer(imGuiContext);
        ImGuiDebugOverlay debugOverlay = new ImGuiDebugOverlay();

        LightEnvironment lightEnv = new LightEnvironment();
        lightEnv.setAmbient(0.08f, 0.08f, 0.08f);
        lightEnv.setShadowMapSize(SHADOW_MAP_SIZE);

        RenderTarget shadowArray = foolsEngine.serviceFactory.createRenderTarget(
                SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, RenderTarget.TARGET_DEPTH, MAX_SHADOW_LAYERS);
        ShadowManager shadowManager = new ShadowManager(shadowArray, depthMaterial, MAX_SHADOW_LAYERS);
        frame.setShadowManager(shadowManager);

        RenderScene scene = new RenderScene();

        InputManager input = new InputManager();
        GLFWKeyBoard keyboard = new GLFWKeyBoard();
        GLFWMouse mouse = new GLFWMouse();

        keyboard.attachEnvironment(win);
        mouse.attachEnvironment(win);
        win.setCursorMode(CursorMode.DISABLED);
        input.register(keyboard);
        input.register(mouse);

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

        input.bind(keyboard, FoolsEngineKeyCode.W, moveForward);
        input.bind(keyboard, FoolsEngineKeyCode.S, moveBackward);
        input.bind(keyboard, FoolsEngineKeyCode.A, moveLeft);
        input.bind(keyboard, FoolsEngineKeyCode.D, moveRight);
        input.bind(keyboard, FoolsEngineKeyCode.SPACE, moveUp);
        input.bind(keyboard, FoolsEngineKeyCode.LEFT_SHIFT, moveDown);

        input.bind(keyboard, FoolsEngineKeyCode.J, ambientUp);
        input.bind(keyboard, FoolsEngineKeyCode.K, ambientDown);

        input.bind(keyboard, FoolsEngineKeyCode.P, spawnDirLight);
        input.bind(keyboard, FoolsEngineKeyCode.O, spawnPointLight);
        input.bind(keyboard, FoolsEngineKeyCode.I, spawnSpotLight);
        input.bind(keyboard, FoolsEngineKeyCode.L, clearLights);
        input.bind(keyboard, FoolsEngineKeyCode.COMMA, spawnShadowDirLight);
        input.bind(keyboard, FoolsEngineKeyCode.N, spawnShadowSpotLight);
        input.bind(keyboard, FoolsEngineKeyCode.ESC, exit);

        input.bind(keyboard, FoolsEngineKeyCode.C, switchDebugWindow);

        input.bind(mouse, FoolsEngineKeyCode.CURSOR, lookDelta);
        input.bind(mouse, FoolsEngineKeyCode.MOUSE_RIGHT, switchMouseMode);

        float moveSpeed = 5.0f;
        float lookSensitivity = 1.0f;
        float yaw = 0;
        float pitch = 0;

        long lastTime = System.nanoTime();

        boolean renderDebug = false;

        java.util.Random rng = new java.util.Random();

        boolean pWasDown = false;
        boolean oWasDown = false;
        boolean iWasDown = false;
        boolean lWasDown = false;
        boolean jWasDown = false;
        boolean kWasDown = false;
        boolean commaWasDown = false;
        boolean nWasDown = false;
        boolean RMBWasDown = false;
        boolean CWasDown = false;

        while (!win.shouldClose()) {
            long currentTime = System.nanoTime();
            float deltaTime = (currentTime - lastTime) / 1e9f;
            lastTime = currentTime;

            input.beginFrame();

            Vector3f forward = new Vector3f(cameraTarget).sub(cameraPos).normalize();
            Vector3f right = new Vector3f(forward).cross(worldUp).normalize();

            if (input.isActionDown(moveForward)) {
                cameraPos.add(new Vector3f(forward).mul(moveSpeed * deltaTime));
            }
            if (input.isActionDown(moveBackward)) {
                cameraPos.sub(new Vector3f(forward).mul(moveSpeed * deltaTime));
            }
            if (input.isActionDown(moveRight)) {
                cameraPos.add(new Vector3f(right).mul(moveSpeed * deltaTime));
            }
            if (input.isActionDown(moveLeft)) {
                cameraPos.sub(new Vector3f(right).mul(moveSpeed * deltaTime));
            }
            if (input.isActionDown(moveUp)) {
                cameraPos.add(new Vector3f(worldUp).mul(moveSpeed * deltaTime));
            }
            if (input.isActionDown(moveDown)) {
                cameraPos.sub(new Vector3f(worldUp).mul(moveSpeed * deltaTime));
            }
            if (input.isActionDown(exit)) {
                break;
            }

            Vector2f mouseDelta = win.getCursorMode() == CursorMode.DISABLED ? input.getActionAxis2DDelta(lookDelta) : new Vector2f(0.0f);
            yaw -= mouseDelta.x * lookSensitivity;
            pitch -= mouseDelta.y * lookSensitivity;
            pitch = Math.min(89.0f, Math.max(-89.0f, pitch));

            Vector3f lookDir = new Vector3f(
                    Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)),
                    Math.sin(Math.toRadians(pitch)),
                    Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))
            ).normalize();

            cameraTarget = new Vector3f(cameraPos).add(lookDir);
            camera.view.identity().lookAt(cameraPos, cameraTarget, worldUp);

            boolean pDown = input.isActionDown(spawnDirLight);
            boolean oDown = input.isActionDown(spawnPointLight);
            boolean iDown = input.isActionDown(spawnSpotLight);
            boolean lDown = input.isActionDown(clearLights);
            boolean jDown = input.isActionDown(ambientUp);
            boolean kDown = input.isActionDown(ambientDown);
            boolean commaDown = input.isActionDown(spawnShadowDirLight);
            boolean nDown = input.isActionDown(spawnShadowSpotLight);
            boolean RMBDown = input.isActionDown(switchMouseMode);
            boolean CDown = input.isActionDown(switchDebugWindow);

            if (pDown && !pWasDown) {
                Vector3f color = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
                lightEnv.add(Light.directional(color, new Vector3f(lookDir)));
            }
            if (oDown && !oWasDown) {
                Vector3f color = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
                lightEnv.add(Light.point(color, new Vector3f(cameraPos), 3.0f));
            }
            if (iDown && !iWasDown) {
                Vector3f color = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
                lightEnv.add(Light.spot(color, new Vector3f(lookDir), new Vector3f(cameraPos), 10f, 10f, 2.0f));
            }
            if (lDown && !lWasDown) {
                lightEnv.clear();
                shadowManager.reset();
            }
            if (jDown && !jWasDown) {
                lightEnv.getAmbient().mul(1.1f);
            }
            if (kDown && !kWasDown) {
                lightEnv.getAmbient().mul(0.9f);
            }

            if (commaDown && !commaWasDown) {
                Vector3f color = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
                Vector3f lightDir = new Vector3f(lookDir);
                Light baseLight = Light.directional(color, lightDir, 1.0f);
                Light dirLight = shadowManager.enableDirLightShadow(baseLight, camera);
                lightEnv.add(dirLight);
            }

            if (nDown && !nWasDown) {
                Vector3f color = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
                Vector3f lightPos = new Vector3f(cameraPos);
                Vector3f lightDir = new Vector3f(lookDir);
                Light baseLight = Light.spot(color, lightDir, lightPos, 10f, 10f, 2.0f);
                Light spotLight = shadowManager.enableSpotLightShadow(baseLight, SPOT_SHADOW_NEAR);
                lightEnv.add(spotLight);
            }

            if (RMBDown && !RMBWasDown) {
                if (win.getCursorMode() == CursorMode.DISABLED)
                    win.setCursorMode(CursorMode.NORMAL);
                else
                    win.setCursorMode(CursorMode.DISABLED);
            }

            if (CDown&&!CWasDown) {
                renderDebug = !renderDebug;
            }

            pWasDown = pDown;
            oWasDown = oDown;
            iWasDown = iDown;
            lWasDown = lDown;
            kWasDown = kDown;
            jWasDown = jDown;
            commaWasDown = commaDown;
            nWasDown = nDown;
            RMBWasDown = RMBDown;
            CWasDown =  CDown;


            scene.clear();

            Vector3f cache = new Vector3f(dragonTransform1.position);

            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    scene.submit(new RenderCommand(dragonMesh, material, new Matrix4f(dragonTransform1.getMatrix())));
                    dragonTransform1.position.add(0, 0, 2);
                    dragonTransform1.markDirty();
                }
                dragonTransform1.position.set(cache);
                dragonTransform1.position.add(3 * i, 0, 0);
                dragonTransform1.markDirty();
            }

            dragonTransform1.position.set(cache);
            dragonTransform1.markDirty();

            scene.setCamera(camera);
            scene.setLighting(lightEnv);
            scene.setBackGroundColor(lightEnv.getAmbient().x, lightEnv.getAmbient().y, lightEnv.getAmbient().z, 1.0f);

            long renderStart = System.nanoTime();
            frame.render(scene);
            float renderTimeMs = (System.nanoTime() - renderStart) / 1e6f;

            if(renderDebug){
                imGuiRenderer.beginFrame();
                debugOverlay.render(scene, shadowManager, deltaTime, renderTimeMs,
                        cameraPos, yaw, pitch, frame.getDrawCallCount());
                imGuiRenderer.endFrame();
            }

            //Logger.debug("FPS: %.1f | Cam: (%.2f, %.2f, %.2f) | Yaw: %.1f | Pitch: %.1f | Lights: %d", 1.0f / deltaTime, cameraPos.x, cameraPos.y, cameraPos.z, yaw, pitch, lightEnv.size());

            input.endFrame();
            win.update();

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        shader.destroy();
        depthShader.destroy();
        shadowManager.destroy();
        imGuiContext.destroy();
        manager.destroyWindow(win, true);
    }
}

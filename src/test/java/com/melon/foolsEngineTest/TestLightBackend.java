package com.melon.foolsEngineTest;

import com.melon.foolsEngine.api.input.*;
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
import com.melon.foolsEngine.util.ObjLoader;
import com.melon.foolsEngine.util.OrthogonalProjection;
import com.melon.foolsEngine.util.PerspectiveProjection;
import com.melon.foolsEngine.util.SignalType;
import org.joml.*;
import org.joml.Math;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestLightBackend {
    static FoolsEngine foolsEngine = FoolsEngine.create(1000, 100, 800, 600);
    private static final int SHADOW_MAP_SIZE = 2048;
    private static final int MAX_SHADOW_LAYERS = 16;
    private static final float FRUSTUM_Z_NEAR = 1.0f;
    private static final float FRUSTUM_Z_FAR = 0.001f;

    public static void main(String[] args) {
        WindowsManager manager = foolsEngine.serviceFactory.getWindowsManager();
        Window win = foolsEngine.mainWindow;
        win.setTitle("Test Light Backend - P:Dir  O:Point  I:Spot  ,:ShadowDir  N:ShadowSpot  L:Clear J:AmbientUp K: AmbientDown ESC:exit");
        win.setSize(2048,1536);

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
        frame.setBackGroundColor(0.05f, 0.05f, 0.1f, 1);

        LightEnvironment lightEnv = new LightEnvironment();
        lightEnv.setAmbient(0.08f, 0.08f, 0.08f);
        lightEnv.setShadowMapSize(SHADOW_MAP_SIZE);

        RenderTarget shadowArray = foolsEngine.serviceFactory.createRenderTarget(
                SHADOW_MAP_SIZE, SHADOW_MAP_SIZE, RenderTarget.TARGET_DEPTH, MAX_SHADOW_LAYERS);
        int nextShadowLayer = 0;

        InputManager input = new InputManager();
        GLFWKeyBoard keyboard = new GLFWKeyBoard();
        GLFWMouse mouse = new GLFWMouse();

        keyboard.attachEnvironment(win);
        mouse.attachEnvironment(win);
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

        input.bind(keyboard, FoolsEngineKeyCode.W, moveForward);
        input.bind(keyboard, FoolsEngineKeyCode.S, moveBackward);
        input.bind(keyboard, FoolsEngineKeyCode.A, moveLeft);
        input.bind(keyboard, FoolsEngineKeyCode.D, moveRight);
        input.bind(keyboard, FoolsEngineKeyCode.SPACE, moveUp);
        input.bind(keyboard, FoolsEngineKeyCode.LEFT_SHIFT, moveDown);

        input.bind(keyboard, FoolsEngineKeyCode.J, ambientUp);
        input.bind(keyboard, FoolsEngineKeyCode.K, ambientDown);

        input.bind(mouse, FoolsEngineKeyCode.CURSOR, lookDelta);

        input.bind(keyboard, FoolsEngineKeyCode.P, spawnDirLight);
        input.bind(keyboard, FoolsEngineKeyCode.O, spawnPointLight);
        input.bind(keyboard, FoolsEngineKeyCode.I, spawnSpotLight);
        input.bind(keyboard, FoolsEngineKeyCode.L, clearLights);
        input.bind(keyboard, FoolsEngineKeyCode.COMMA, spawnShadowDirLight);
        input.bind(keyboard, FoolsEngineKeyCode.N, spawnShadowSpotLight);
        input.bind(keyboard, FoolsEngineKeyCode.ESC, exit);

        float moveSpeed = 5.0f;
        float lookSensitivity = 1.0f;
        float yaw = 0;
        float pitch = 0;

        long lastTime = System.nanoTime();

        List<Camera> shadowCameras = new ArrayList<>();
        List<Integer> shadowLayers = new ArrayList<>();
        List<Vector3f> shadowLightDirs = new ArrayList<>();
        java.util.Random rng = new java.util.Random();

        boolean pWasDown = false;
        boolean oWasDown = false;
        boolean iWasDown = false;
        boolean lWasDown = false;
        boolean jWasDown = false;
        boolean kWasDown = false;
        boolean commaWasDown = false;
        boolean nWasDown = false;

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

            Vector2f mouseDelta = input.getActionAxis2DDelta(lookDelta);
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
                lightEnv.add(Light.spot(color, new Vector3f(lookDir), new Vector3f(cameraPos), 0.91f, 2.0f));
            }
            if (lDown && !lWasDown) {
                lightEnv.clear();
                shadowCameras.clear();
                shadowLayers.clear();
                shadowLightDirs.clear();
                nextShadowLayer = 0;
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
                int layer = nextShadowLayer++;

                Camera shadowCam = new Camera(new Matrix4f(), new Matrix4f());
                shadowCameras.add(shadowCam);
                shadowLayers.add(layer);
                shadowLightDirs.add(new Vector3f(lightDir));

                Matrix4f lsMatrix = new Matrix4f();
                lightEnv.add(Light.directional(color, lightDir, 1.0f,
                        Collections.singletonList(shadowArray), Collections.singletonList(lsMatrix), layer));
            }

            if (nDown && !nWasDown) {
                Vector3f color = new Vector3f(rng.nextFloat(), rng.nextFloat(), rng.nextFloat());
                Vector3f lightPos = new Vector3f(cameraPos);
                Vector3f lightDir = new Vector3f(lookDir);
                int layer = nextShadowLayer++;

                Matrix4f shadowView = new Matrix4f().lookAt(lightPos,
                        new Vector3f(lightPos).add(lightDir), worldUp);
                PerspectiveProjection spotProj = new PerspectiveProjection(50f, 1.0f, 0.1f);
                Matrix4f lightSpace = new Matrix4f(spotProj.get(new Matrix4f())).mul(shadowView);
                shadowCameras.add(new Camera(shadowView, spotProj.get(new Matrix4f())));
                shadowLayers.add(layer);
                shadowLightDirs.add(null);

                lightEnv.add(Light.spot(color, lightDir, lightPos, 0.91f, 2.0f,
                        Collections.singletonList(shadowArray), Collections.singletonList(lightSpace), layer));
            }

            pWasDown = pDown;
            oWasDown = oDown;
            iWasDown = iDown;
            lWasDown = lDown;
            kWasDown = kDown;
            jWasDown = jDown;
            commaWasDown = commaDown;
            nWasDown = nDown;


            frame.beginFrame();

            Vector3f cache = new Vector3f(dragonTransform1.position);

            for(int i=0;i<10;i++){
                for(int j=0;j<10;j++){
                    frame.submit(new RenderCommand(dragonMesh, material, new Matrix4f(dragonTransform1.getMatrix())));
                    dragonTransform1.position.add(0,0,2);
                    dragonTransform1.markDirty();
                }
                dragonTransform1.position.set(cache);
                dragonTransform1.position.add(3*i,0,0);
                dragonTransform1.markDirty();
            }

            dragonTransform1.position.set(cache) ;
            dragonTransform1.markDirty();

            int shadowIdx = 0;
            List<Light> lights = lightEnv.getLights();
            for (int li = 0; li < lights.size(); li++) {
                Light light = lights.get(li);
                if (light.castsShadow()) {
                    if (light.type == Light.DIRECTIONAL) {
                        Vector3f dir = shadowLightDirs.get(shadowIdx);
                        if (dir != null) {
                            updateDirLightShadowCam(shadowCameras.get(shadowIdx), dir, camera);
                        }
                        light.lightSpaceMatrices.get(0).set(shadowCameras.get(shadowIdx).vp());
                    }
                    frame.setCamera(shadowCameras.get(shadowIdx));
                    frame.endFrame(shadowArray, depthMaterial, shadowLayers.get(shadowIdx));
                    shadowIdx++;
                }
            }

            frame.setCamera(camera);
            frame.applyLightEnvironment(lightEnv);
            frame.setBackGroundColor(lightEnv.getAmbient().x, lightEnv.getAmbient().y, lightEnv.getAmbient().z, 1.0f);
            frame.endFrame();

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
        shadowArray.destroy();
        manager.destroyWindow(win, true);
    }

    private static void updateDirLightShadowCam(Camera shadowCam, Vector3f lightDir, Camera mainCamera) {
        Vector4f[] ndc = {
            new Vector4f(-1, -1, FRUSTUM_Z_NEAR, 1), new Vector4f(1, -1, FRUSTUM_Z_NEAR, 1),
            new Vector4f(-1,  1, FRUSTUM_Z_NEAR, 1), new Vector4f(1,  1, FRUSTUM_Z_NEAR, 1),
            new Vector4f(-1, -1, FRUSTUM_Z_FAR,  1), new Vector4f(1, -1, FRUSTUM_Z_FAR,  1),
            new Vector4f(-1,  1, FRUSTUM_Z_FAR,  1), new Vector4f(1,  1, FRUSTUM_Z_FAR,  1),
        };

        Matrix4f invVP = new Matrix4f(mainCamera.vp());
        invVP.invert();

        Vector3f[] corners = new Vector3f[8];
        Vector3f center = new Vector3f();
        for (int i = 0; i < 8; i++) {
            Vector4f w = ndc[i].mul(invVP, new Vector4f());
            float invW = 1f / w.w;
            corners[i] = new Vector3f(w.x * invW, w.y * invW, w.z * invW);
            center.add(corners[i]);
        }
        center.div(8);

        Vector3f dir = new Vector3f(lightDir).normalize();
        Vector3f up = new Vector3f(0, 1, 0);
        if (Math.abs(dir.y) > 0.99f) up.set(1, 0, 0);
        Vector3f lightPos = new Vector3f(center).add(new Vector3f(dir).mul(-30));

        Matrix4f lightView = new Matrix4f().lookAt(lightPos, center, up);

        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (Vector3f c : corners) {
            Vector4f ls = new Vector4f(c, 1).mul(lightView);
            minX = Math.min(minX, ls.x); maxX = Math.max(maxX, ls.x);
            minY = Math.min(minY, ls.y); maxY = Math.max(maxY, ls.y);
            minZ = Math.min(minZ, -ls.z); maxZ = Math.max(maxZ, -ls.z);
        }

        float halfW = (maxX - minX) * 0.5f + 2f;
        float halfH = (maxY - minY) * 0.5f + 2f;
        OrthogonalProjection ortho = new OrthogonalProjection(halfW, halfH, Math.max(minZ - 5, 0.01f), maxZ + 5);

        shadowCam.view.set(lightView);
        shadowCam.projection = ortho.get(new Matrix4f());
    }
}

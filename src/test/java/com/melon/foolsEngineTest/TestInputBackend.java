package com.melon.foolsEngineTest;

import com.melon.foolsEngine.api.input.*;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.resource.*;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.Window;
import com.melon.foolsEngine.api.windows.WindowsManager;
import com.melon.foolsEngine.backend.OpenGL.GLFWKeyBoard;
import com.melon.foolsEngine.backend.OpenGL.GLFWMouse;
import com.melon.foolsEngine.core.ECS.basicComponents.Transform;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.ObjLoader;
import com.melon.foolsEngine.util.PerspectiveProjection;
import com.melon.foolsEngine.util.SignalType;
import org.joml.*;
import org.joml.Math;

import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.*;

public class TestInputBackend {
    static FoolsEngine foolsEngine = FoolsEngine.create(1000, 100, 800, 600);

    public static void main(String[] args) {
        WindowsManager manager = foolsEngine.serviceFactory.getWindowsManager();
        Window win = foolsEngine.mainWindow;
        win.setTitle("Test Input Backend - WASD + Mouse Look");

        Mesh dragonMesh = foolsEngine.serviceFactory.getMesh();
        dragonMesh.upload(ObjLoader.loadMesh(Path.of("src/test/resources/shaders/model/dragon.obj")));

        ShaderProgram shader = foolsEngine.serviceFactory.getShaderProgram();
        shader.load(Path.of("src/main/resources/shader/vsh/main_vsh.glsl"), Path.of("src/main/resources/shader/fsh/main_fsh.glsl"));
        Material material = new Material(shader);
        Texture texture = foolsEngine.serviceFactory.getTexture();
        texture.upload(Path.of("src/test/resources/textures/test2.png"));
        material.set("textureSampler", texture);

        Transform dragonTransform = new Transform(new Vector3f(0, 0, 0), new Quaternionf(), new Vector3f(0.1f, 0.1f, 0.1f));

        PerspectiveProjection proj = new PerspectiveProjection(foolsEngine.FOV, foolsEngine.aspect, foolsEngine.Z_NEAR);
        Vector3f cameraPos = new Vector3f(0, 0, -5);
        Vector3f cameraTarget = new Vector3f(0, 0, 0);
        Vector3f worldUp = new Vector3f(0, 1, 0);
        Camera camera = new Camera(
                new Matrix4f().lookAt(cameraPos, cameraTarget, worldUp),
                proj.get(new Matrix4f())
        );

        win.show();
        RenderFrame frame = foolsEngine.frame;
        frame.init();
        frame.setBackGroundColor(0.2f, 0.3f, 0.35f, 1);

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

        input.bind(keyboard, FoolsEngineKeyCode.W, moveForward);
        input.bind(keyboard, FoolsEngineKeyCode.S, moveBackward);
        input.bind(keyboard, FoolsEngineKeyCode.A, moveLeft);
        input.bind(keyboard, FoolsEngineKeyCode.D, moveRight);
        input.bind(keyboard, FoolsEngineKeyCode.SPACE, moveUp);
        input.bind(keyboard, FoolsEngineKeyCode.LEFT_SHIFT, moveDown);

        input.bind(mouse, FoolsEngineKeyCode.CURSOR, lookDelta);



        float moveSpeed = 5.0f;
        float lookSensitivity = 15.0f;
        float yaw = 0;
        float pitch = 0;

        long lastTime = System.nanoTime();

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

            Vector2f mouseDelta = input.getActionAxis2DDelta(lookDelta);
            yaw -= mouseDelta.x * lookSensitivity * deltaTime;
            pitch -= mouseDelta.y * lookSensitivity * deltaTime;
            pitch = Math.min(89.0f, Math.max(-89.0f, pitch));

            Vector3f lookDir = new Vector3f(
                    Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)),
                    Math.sin(Math.toRadians(pitch)),
                    Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))
            ).normalize();

            cameraTarget = new Vector3f(cameraPos).add(lookDir);
            camera.view.identity().lookAt(cameraPos, cameraTarget, worldUp);

            frame.beginFrame();
            frame.setCamera(camera);
            frame.submit(new RenderCommand(dragonMesh, material, dragonTransform.getMatrix()));
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
        manager.destroyWindow(win, true);
    }
}

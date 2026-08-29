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

import com.melon.foolsEngine.api.input.Action;
import com.melon.foolsEngine.api.input.FoolsEngineKeyCode;
import com.melon.foolsEngine.api.input.InputManager;
import com.melon.foolsEngine.api.rendering.render.RenderCommand;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.api.rendering.pipeline.ShaderPass;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.Material;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.api.rendering.shader.BuiltinShaders;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.Window;
import com.melon.foolsEngine.api.windows.WindowsManager;
import com.melon.foolsEngine.core.bootstrap.EngineBoot;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.CursorMode;
import com.melon.foolsEngine.util.ObjLoader;
import com.melon.foolsEngine.util.PerspectiveProjection;
import com.melon.foolsEngine.util.SignalType;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Math;

import java.nio.file.Path;

/**
 * Demonstrates the alpha-blended transparent pass of the multi-pass pipeline.
 *
 * <p>Renders opaque dragons plus several semi-transparent "glass" dragons at
 * different depths. The transparent pass enables GL blending, disables depth
 * writes, and sorts the glass objects back-to-front by camera distance.</p>
 */
public class TestTransparentBackend {
    static FoolsEngine foolsEngine = EngineBoot.create(1000, 100, 800, 600, false);

    private static Matrix4f model(float x, float y, float z, float scale) {
        return new Matrix4f().translation(x, y, z).scale(scale);
    }

    public static void main(String[] args) {
        WindowsManager manager = foolsEngine.serviceFactory.getWindowsManager();
        Window win = foolsEngine.mainWindow;
        win.setTitle("Test Transparent Backend - WASD move, mouse look, ESC exit");
        win.setSize(1600, 1200);

        Mesh dragonMesh = foolsEngine.serviceFactory.getMesh();
        dragonMesh.upload(ObjLoader.loadMesh(Path.of("src/test/resources/shaders/model/dragon.obj")));

        BuiltinShaders builtin = BuiltinShaders.load(foolsEngine.serviceFactory);
        ShaderProgram shader = builtin.main();

        Texture texture = foolsEngine.serviceFactory.getTexture();
        texture.upload(Path.of("src/test/resources/textures/test2.png"));

        Material opaque = new Material(shader);
        opaque.set("textureSampler", texture);

        Material glass = new Material(shader);
        glass.set("textureSampler", texture);
        glass.set("alpha", 0.35f);
        glass.setTransparent(true);

        LightEnvironment lightEnv = new LightEnvironment();
        lightEnv.setAmbient(0.15f, 0.15f, 0.15f);
        lightEnv.add(Light.directional(
                new Vector3f(1.0f, 1.0f, 1.0f),
                new Vector3f(0.5f, -1.0f, -0.3f)));

        PerspectiveProjection proj = new PerspectiveProjection(
                foolsEngine.FOV, foolsEngine.aspect, foolsEngine.Z_NEAR);
        Vector3f cameraPos = new Vector3f(0, 0, 15);
        Vector3f cameraTarget = new Vector3f(0, 0, 0);
        Vector3f worldUp = new Vector3f(0, 1, 0);
        Camera camera = new Camera(
                new Matrix4f().lookAt(cameraPos, cameraTarget, worldUp),
                proj.get(new Matrix4f()));

        win.show();
        RenderFrame frame = foolsEngine.frame;
        frame.init();

        RenderScene scene = new RenderScene();

        InputManager input = foolsEngine.serviceFactory.createInputManager(win);
        win.setCursorMode(CursorMode.DISABLED);

        Action moveForward = () -> SignalType.BUTTON;
        Action moveBackward = () -> SignalType.BUTTON;
        Action moveLeft = () -> SignalType.BUTTON;
        Action moveRight = () -> SignalType.BUTTON;
        Action moveUp = () -> SignalType.BUTTON;
        Action moveDown = () -> SignalType.BUTTON;
        Action lookDelta = () -> SignalType.AXIS_2DDel;
        Action exit = () -> SignalType.BUTTON;

        input.bind(input.getKeyboard(), FoolsEngineKeyCode.W, moveForward);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.S, moveBackward);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.A, moveLeft);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.D, moveRight);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.SPACE, moveUp);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.LEFT_SHIFT, moveDown);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.ESC, exit);
        input.bind(input.getMouse(), FoolsEngineKeyCode.CURSOR, lookDelta);

        float moveSpeed = 5.0f;
        float lookSensitivity = 1.0f;
        float yaw = 0;
        float pitch = 0;

        long lastTime = System.nanoTime();

        while (!win.shouldClose()) {
            long currentTime = System.nanoTime();
            float deltaTime = (currentTime - lastTime) / 1e9f;
            lastTime = currentTime;

            input.updateFromPolledInputs();

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

            Vector2f mouseDelta = win.getCursorMode() == CursorMode.DISABLED
                    ? input.getActionAxis2DDelta(lookDelta) : new Vector2f(0.0f);
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

            scene.clear();

            // Opaque dragons (behind the glass).
            scene.submit(new RenderCommand(dragonMesh, opaque, model(-2.0f, -0.5f, -2.0f, 0.12f)));
            scene.submit(new RenderCommand(dragonMesh, opaque, model(2.0f, -0.5f, -3.0f, 0.12f)));

            // Semi-transparent glass dragons at increasing depth along the view axis.
            scene.submit(new RenderCommand(dragonMesh, glass, model(0.0f, 0.0f, 1.0f, 0.12f)));
            scene.submit(new RenderCommand(dragonMesh, glass, model(0.6f, 0.0f, 3.0f, 0.12f)));
            scene.submit(new RenderCommand(dragonMesh, glass, model(-0.6f, 0.0f, 5.0f, 0.12f)));

            scene.setCamera(camera);
            scene.setLighting(lightEnv);
            scene.setBackGroundColor(0.05f, 0.05f, 0.1f, 1.0f);

            scene.submitPass(ShaderPass.core().build());
            scene.submitPass(ShaderPass.core().transparent().build());

            frame.render(scene);

            input.clearPolledInputs();
            win.update();

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        builtin.close();
        lightEnv.destroy();
        manager.destroyWindow(win, true);
    }
}

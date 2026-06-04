package com.melon.foolsEngine.util.imgui;

import com.melon.foolsEngine.api.rendering.resource.Light;
import com.melon.foolsEngine.api.rendering.resource.LightEnvironment;
import com.melon.foolsEngine.api.rendering.resource.RenderScene;
import com.melon.foolsEngine.api.rendering.resource.ShadowManager;
import com.melon.foolsEngine.util.LightType;
import imgui.ImGuiIO;
import org.joml.Vector3f;

import imgui.ImGui;
import imgui.flag.ImGuiCond;

public class ImGuiDebugOverlay {

    public void render(RenderScene scene, ShadowManager shadowManager,
                       float deltaTime, float renderTimeMs,
                       Vector3f cameraPos, float yaw, float pitch,
                       int drawCallCount) {

        ImGui.setNextWindowSize(320, 360, ImGuiCond.Once);
        ImGui.begin("FoolsEngine Debug");

        float fps = 1.0f / Math.max(deltaTime, 0.0001f);
        ImGui.text(String.format("FPS: %.1f (%.2f ms)", fps, deltaTime * 1000f));
        ImGuiIO io = ImGui.getIO();

        ImGui.text("MouseX: " + io.getMousePosX());
        ImGui.text("MouseY: " + io.getMousePosY());

        ImGui.text("WantCaptureMouse: " + io.getWantCaptureMouse());

        ImGui.separator();

        if (ImGui.collapsingHeader("Camera")) {
            ImGui.text(String.format("Position: (%.2f, %.2f, %.2f)", cameraPos.x, cameraPos.y, cameraPos.z));
            ImGui.text(String.format("Yaw: %.1f  Pitch: %.1f", yaw, pitch));
        }

        ImGui.separator();

        LightEnvironment lightEnv = scene != null ? scene.getLighting() : null;
        if (lightEnv != null && ImGui.collapsingHeader("Lights")) {
            int dirCount = 0, pointCount = 0, spotCount = 0, shadowCount = 0;
            for (Light l : lightEnv.getLights()) {
                if (l.type == LightType.PARALLEL) {
                    dirCount++;
                } else if (l.type == LightType.POINT) {
                    pointCount++;
                } else if (l.type == LightType.SPOT) {
                    spotCount++;
                }
                if (l.castsShadow()) {
                    shadowCount++;
                }
            }
            int total = lightEnv.size();
            ImGui.text(String.format("Total: %d", total));
            ImGui.text(String.format("Directional: %d  Point: %d  Spot: %d", dirCount, pointCount, spotCount));
            ImGui.text(String.format("Casting Shadows: %d", shadowCount));

            Vector3f ambient = lightEnv.getAmbient();
            ImGui.text(String.format("Ambient: (%.3f, %.3f, %.3f)", ambient.x, ambient.y, ambient.z));
        }

        ImGui.separator();

        if (shadowManager != null && ImGui.collapsingHeader("Shadows")) {
            int layers = shadowManager.getCurrentLayerCount();
            int maxLayers = shadowManager.getMaxLayers();
            ImGui.text(String.format("Layers Used: %d / %d", layers, maxLayers));

            if (lightEnv != null) {
                ImGui.text(String.format("Map Size: %d", lightEnv.getShadowMapSize()));
            }
        }

        ImGui.separator();

        if (ImGui.collapsingHeader("Renderer")) {
            ImGui.text(String.format("Draw Calls: %d", drawCallCount >= 0 ? drawCallCount : -1));
            ImGui.text(String.format("Render Pass: %.2f ms", renderTimeMs));
        }

        ImGui.end();
    }
}

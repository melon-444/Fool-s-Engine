package com.melon.foolsEngine.api.rendering.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RenderScene {

    private Camera camera;
    private LightEnvironment lightEnv;
    private final List<RenderCommand> commands = new ArrayList<>();
    private float bgR = 0.05f;
    private float bgG = 0.05f;
    private float bgB = 0.1f;
    private float bgA = 1.0f;

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setLighting(LightEnvironment env) {
        this.lightEnv = env;
    }

    public LightEnvironment getLighting() {
        return lightEnv;
    }

    public void submit(RenderCommand command) {
        commands.add(command);
    }

    public List<RenderCommand> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    public void setBackGroundColor(float r, float g, float b, float a) {
        this.bgR = r;
        this.bgG = g;
        this.bgB = b;
        this.bgA = a;
    }

    public float getBgR() {
        return bgR;
    }

    public float getBgG() {
        return bgG;
    }

    public float getBgB() {
        return bgB;
    }

    public float getBgA() {
        return bgA;
    }

    public void clear() {
        commands.clear();
    }
}

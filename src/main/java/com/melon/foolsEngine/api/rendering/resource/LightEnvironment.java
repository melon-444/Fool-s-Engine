package com.melon.foolsEngine.api.rendering.resource;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LightEnvironment {

    private final List<Light> lights = new ArrayList<>();
    private final Vector3f ambientColor = new Vector3f(0.06f, 0.06f, 0.06f);

    public void add(Light light) {
        lights.add(light);
    }

    public void remove(Light light) {
        lights.remove(light);
    }

    public void clear() {
        lights.clear();
    }

    public void setAmbient(float r, float g, float b) {
        ambientColor.set(r, g, b);
    }

    public void setAmbient(Vector3f color) {
        ambientColor.set(color);
    }

    public Vector3f getAmbient() {
        return ambientColor;
    }

    public List<Light> getLights() {
        return Collections.unmodifiableList(lights);
    }

    public int size() {
        return lights.size();
    }
}

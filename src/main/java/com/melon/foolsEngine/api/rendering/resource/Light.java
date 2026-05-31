package com.melon.foolsEngine.api.rendering.resource;

import org.joml.Vector3f;

public class Light {

    public static final int DIRECTIONAL = 0;
    public static final int POINT = 1;
    public static final int SPOT = 2;

    public final int type;
    public final Vector3f color;
    public final Vector3f direction;
    public final Vector3f position;
    public final float intensity;
    public final float cutOff;

    private Light(int type, Vector3f color, Vector3f direction, Vector3f position, float intensity, float cutOff) {
        this.type = type;
        this.color = color;
        this.direction = direction;
        this.position = position;
        this.intensity = intensity;
        this.cutOff = cutOff;
    }

    public static Light directional(Vector3f color, Vector3f direction) {
        return new Light(DIRECTIONAL, color, new Vector3f(direction).normalize(), new Vector3f(), 1.0f, 0f);
    }

    public static Light directional(Vector3f color, Vector3f direction, float intensity) {
        return new Light(DIRECTIONAL, color, new Vector3f(direction).normalize(), new Vector3f(), intensity, 0f);
    }

    public static Light point(Vector3f color, Vector3f position) {
        return new Light(POINT, color, new Vector3f(), new Vector3f(position), 1.0f, 0f);
    }

    public static Light point(Vector3f color, Vector3f position, float intensity) {
        return new Light(POINT, color, new Vector3f(), new Vector3f(position), intensity, 0f);
    }

    public static Light spot(Vector3f color, Vector3f direction, Vector3f position, float cutOff) {
        return new Light(SPOT, color, new Vector3f(direction).normalize(), new Vector3f(position), 1.0f, cutOff);
    }

    public static Light spot(Vector3f color, Vector3f direction, Vector3f position, float cutOff, float intensity) {
        return new Light(SPOT, color, new Vector3f(direction).normalize(), new Vector3f(position), intensity, cutOff);
    }
}

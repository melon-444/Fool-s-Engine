package com.melon.foolsEngine.api.rendering.resource;

import com.melon.foolsEngine.api.rendering.render.RenderTarget;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    public final List<RenderTarget> shadowMaps;
    public final List<Matrix4f> lightSpaceMatrices;
    public final int shadowLayer;

    private Light(int type, Vector3f color, Vector3f direction, Vector3f position, float intensity, float cutOff,
                  List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices, int shadowLayer) {
        this.type = type;
        this.color = color;
        this.direction = direction;
        this.position = position;
        this.intensity = intensity;
        this.cutOff = cutOff;
        this.shadowMaps = shadowMaps != null ? Collections.unmodifiableList(new ArrayList<>(shadowMaps)) : Collections.emptyList();
        this.lightSpaceMatrices = lightSpaceMatrices != null ? Collections.unmodifiableList(new ArrayList<>(lightSpaceMatrices)) : Collections.emptyList();
        this.shadowLayer = shadowLayer;
    }

    public static Light directional(Vector3f color, Vector3f direction) {
        return new Light(DIRECTIONAL, color, new Vector3f(direction).normalize(), new Vector3f(), 1.0f, 0f, null, null, -1);
    }

    public static Light directional(Vector3f color, Vector3f direction, float intensity) {
        return new Light(DIRECTIONAL, color, new Vector3f(direction).normalize(), new Vector3f(), intensity, 0f, null, null, -1);
    }

    public static Light directional(Vector3f color, Vector3f direction, float intensity,
                                    List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices, int shadowLayer) {
        return new Light(DIRECTIONAL, color, new Vector3f(direction).normalize(), new Vector3f(), intensity, 0f, shadowMaps, lightSpaceMatrices, shadowLayer);
    }

    public static Light point(Vector3f color, Vector3f position) {
        return new Light(POINT, color, new Vector3f(), new Vector3f(position), 1.0f, 0f, null, null, -1);
    }

    public static Light point(Vector3f color, Vector3f position, float intensity) {
        return new Light(POINT, color, new Vector3f(), new Vector3f(position), intensity, 0f, null, null, -1);
    }

    public static Light point(Vector3f color, Vector3f position, float intensity,
                              List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices, int shadowLayer) {
        return new Light(POINT, color, new Vector3f(), new Vector3f(position), intensity, 0f, shadowMaps, lightSpaceMatrices, shadowLayer);
    }

    public static Light spot(Vector3f color, Vector3f direction, Vector3f position, float cutOff) {
        return new Light(SPOT, color, new Vector3f(direction).normalize(), new Vector3f(position), 1.0f, cutOff, null, null, -1);
    }

    public static Light spot(Vector3f color, Vector3f direction, Vector3f position, float cutOff, float intensity) {
        return new Light(SPOT, color, new Vector3f(direction).normalize(), new Vector3f(position), intensity, cutOff, null, null, -1);
    }

    public static Light spot(Vector3f color, Vector3f direction, Vector3f position, float cutOff, float intensity,
                             List<RenderTarget> shadowMaps, List<Matrix4f> lightSpaceMatrices, int shadowLayer) {
        return new Light(SPOT, color, new Vector3f(direction).normalize(), new Vector3f(position), intensity, cutOff, shadowMaps, lightSpaceMatrices, shadowLayer);
    }

    public boolean castsShadow() {
        return !shadowMaps.isEmpty() && !lightSpaceMatrices.isEmpty() && shadowLayer >= 0;
    }
}

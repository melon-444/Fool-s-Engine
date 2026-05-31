package com.melon.foolsEngine.api.rendering.resource;

import org.joml.Matrix4f;

public record RenderCommand(
        Mesh mesh,
        Material material,
        Matrix4f transform
) {}

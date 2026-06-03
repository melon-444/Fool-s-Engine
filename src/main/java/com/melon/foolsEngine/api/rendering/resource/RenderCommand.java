package com.melon.foolsEngine.api.rendering.resource;

import org.joml.Matrix4f;

/**
 * A single draw command: what mesh to render, with what material, at what transform.
 * Submitted to {@link RenderScene}.
 *
 * @param mesh the mesh to draw
 * @param material the material (shader + parameters)
 * @param transform the model matrix
 */
public record RenderCommand(
        Mesh mesh,
        Material material,
        Matrix4f transform
) {}

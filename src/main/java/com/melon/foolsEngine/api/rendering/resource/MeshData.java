package com.melon.foolsEngine.api.rendering.resource;

import com.melon.foolsEngine.util.VertexLayout;

/**
 * Raw mesh data ready for GPU upload.
 *
 * @param vertices interleaved vertex data (positions, UVs, normals, etc.)
 * @param indices index buffer data
 * @param layout the vertex attribute layout describing the vertex format
 */
public record MeshData(
        float[] vertices,
        int[] indices,
        VertexLayout layout
) {}

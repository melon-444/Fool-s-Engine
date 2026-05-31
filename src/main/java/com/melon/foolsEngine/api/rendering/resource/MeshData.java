package com.melon.foolsEngine.api.rendering.resource;

import com.melon.foolsEngine.util.VertexLayout;

public record MeshData(
        float[] vertices,
        int[] indices,
        VertexLayout layout
) {}
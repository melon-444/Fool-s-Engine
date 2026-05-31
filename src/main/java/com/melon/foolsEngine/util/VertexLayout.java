package com.melon.foolsEngine.util;

import java.util.ArrayList;
import java.util.List;

public class VertexLayout {
    public record VertexAttribute(
            int location,   //  shader layout
            int size,       // vec3=3
            int offset
    ) {}

    private final List<VertexAttribute> attributes = new ArrayList<>();
    private int stride = 0;

    public VertexLayout add(int location, int size) {
        attributes.add(new VertexAttribute(location, size, stride));
        stride += size;
        return this;
    }

    public List<VertexAttribute> attributes() {
        return attributes;
    }

    public int stride() {
        return stride;
    }
}

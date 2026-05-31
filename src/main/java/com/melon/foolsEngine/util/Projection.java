package com.melon.foolsEngine.util;

import org.joml.Matrix4f;

public interface Projection {
    Matrix4f get(Matrix4f dest);
}

package com.melon.foolsEngine.api.input;

import org.joml.Vector2f;

public interface InputDevice<E> {
    boolean getButton(FoolsEngineKeyCode id);
    float getAxis1D(FoolsEngineKeyCode id);
    float getAxis1DDelta(FoolsEngineKeyCode id);
    Vector2f getAxis2D(FoolsEngineKeyCode id);
    Vector2f getAxis2DDelta(FoolsEngineKeyCode id);

    void beginFrame();
    void endFrame();

    void attachEnvironment(E env);
    void detachEnvironment();
}
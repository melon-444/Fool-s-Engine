package com.melon.foolsEngine.api.input;

import org.joml.Vector2f;

/**
 * Abstraction for an input hardware device (keyboard, mouse, gamepad, etc.).
 * An environment (such as a {@link com.melon.foolsEngine.api.windows.Window}) is attached
 * to receive raw input events.
 *
 * @param <E> the environment type required by this device
 */
public interface InputDevice<E> {
    /** @return whether the button/key identified by {@code id} is currently held down */
    boolean getButton(FoolsEngineKeyCode id);
    /** @return the current 1D axis value */
    float getAxis1D(FoolsEngineKeyCode id);
    /** @return the frame-to-frame delta of the 1D axis value */
    float getAxis1DDelta(FoolsEngineKeyCode id);
    /** @return the current 2D axis value */
    Vector2f getAxis2D(FoolsEngineKeyCode id);
    /** @return the frame-to-frame delta of the 2D axis value */
    Vector2f getAxis2DDelta(FoolsEngineKeyCode id);

    /** Called at the start of each frame by the input manager */
    void beginFrame();
    /** Called at the end of each frame to clear accumulated state */
    void endFrame();

    /** Attaches this device to an environment (e.g., a GLFW window for callbacks) */
    void attachEnvironment(E env);
    /** Detaches and releases environment resources */
    void detachEnvironment();
}

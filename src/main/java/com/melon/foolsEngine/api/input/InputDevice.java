// foolsEngine - A custom 3D game engine in Java
// Copyright (C) 2026  melon_444
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.

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

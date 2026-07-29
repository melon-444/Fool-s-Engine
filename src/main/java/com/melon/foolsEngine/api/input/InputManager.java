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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InputManager {
    private final List<InputDevice<?>> inputDevices = new ArrayList<>();
    private final InputState state = new InputState();
    private final ActionMapping map = new ActionMapping();

    private InputDevice<?> keyboard;
    private InputDevice<?> mouse;

    public void register(InputDevice<?> inputDevice) {
        inputDevices.add(inputDevice);
        map.register(inputDevice);
    }

    public void registerKeyboard(InputDevice<?> kb) {
        keyboard = kb;
        register(kb);
    }

    public void registerMouse(InputDevice<?> m) {
        mouse = m;
        register(m);
    }

    public InputDevice<?> getKeyboard() {
        return keyboard;
    }

    public InputDevice<?> getMouse() {
        return mouse;
    }

    @SuppressWarnings("unchecked")
    public <T extends InputDevice<?>> T getDevice(Class<T> type) {
        for (InputDevice<?> d : inputDevices) {
            if (type.isInstance(d)) return (T) d;
        }
        return null;
    }

    public void bind(InputDevice<?> inputDevice, FoolsEngineKeyCode id, Action action) {
        map.bind(inputDevice, id, action);
    }

    /**
     * call this method at the start of the game frame loop
     */
    public void beginFrame() {
        inputDevices.forEach(InputDevice::beginFrame);
        state.clearSignalCache();
        for (InputDevice<?> inputDevice : inputDevices) {
            Map<FoolsEngineKeyCode, List<Action>> currentMap = map.get(inputDevice);
            if (currentMap == null) continue;
            for (Map.Entry<FoolsEngineKeyCode, List<Action>> entry : currentMap.entrySet()) {
                FoolsEngineKeyCode id = entry.getKey();
                if (id == FoolsEngineKeyCode.NULL) continue;
                for (Action action : entry.getValue()) {
                    switch (action.Type()) {
                        case BUTTON:
                            state.setDown(action, inputDevice.getButton(id));
                            state.setPressed(action, state.isDown(action) && !state.isDownLast(action));
                            break;
                        case AXIS_1D:
                            state.setAxis1D(action, inputDevice.getAxis1D(id));
                            break;
                        case AXIS_2D:
                            state.setAxis2D(action, inputDevice.getAxis2D(id).x, inputDevice.getAxis2D(id).y);
                            break;
                        case AXIS_1DDel:
                            state.setAxis1DDelta(action, inputDevice.getAxis1DDelta(id));
                            break;
                        case AXIS_2DDel:
                            state.setAxis2DDelta(action, inputDevice.getAxis2DDelta(id).x, inputDevice.getAxis2DDelta(id).y);
                            break;
                        default:
                            throw new RuntimeException("Unsupported action type");
                    }
                }
            }
        }
    }

    /**
     * call this method at the end of the game frame loop
     */
    public void endFrame() {
        inputDevices.forEach(InputDevice::endFrame);
    }

    /**
     *  Does the action keep pressing down
     * @param action the action want to detect
     * @return whether the action is activated
     */
    public boolean isActionDown(Action action) {
        return state.isDown(action);
    }

    /**
     *  Did the action trigger once
     * @param action the action want to detect
     * @return whether the action is triggered once
     */
    public boolean isActionPressed(Action action) {
        return state.isPressed(action);
    }

    /**
     *  Does the action slide
     * @param action the action want to detect
     * @return the value of sliding in current frame
     */
    public float getActionAxis1D(Action action) {
        return state.getAxis1D(action);
    }

    /**
     *  Does the action slide in 2 dimensions
     * @param action the action want to detect
     * @return the value of sliding in current frame
     */
    public Vector2f getActionAxis2D(Action action) {
        return state.getAxis2D(action);
    }

    /**
     *  Does the action slide(derivative)
     * @param action the action want to detect
     * @return the value of sliding in current frame
     */
    public float getActionAxis1DDelta(Action action) {
        return state.getAxis1DDelta(action);
    }

    /**
     *  Does the action slide in 2 dimensions(derivative)
     * @param action the action want to detect
     * @return the value of sliding in current frame
     */
    public Vector2f getActionAxis2DDelta(Action action) {
        return state.getAxis2DDelta(action);
    }
}

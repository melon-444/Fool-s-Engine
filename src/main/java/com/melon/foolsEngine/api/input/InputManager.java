package com.melon.foolsEngine.api.input;

import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InputManager {
    private final List<InputDevice<?>> inputDevices = new ArrayList<>();
    private final InputState state = new InputState();
    private final ActionMapping map = new ActionMapping();

    public void register(InputDevice<?> inputDevice) {
        inputDevices.add(inputDevice);
        map.register(inputDevice);
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
        for(InputDevice<?> inputDevice : inputDevices) {
            Map<FoolsEngineKeyCode,Action> currentMap = map.get(inputDevice);
            for(FoolsEngineKeyCode id: currentMap.keySet()) {
                Action action = currentMap.get(id);
                switch (action.Type()){
                    case BUTTON :
                        state.setDown(action,inputDevice.getButton(id));
                        state.setPressed(action,state.isDown(action)&&!state.isDownLast(action));
                        break;
                    case AXIS_1D :
                        state.setAxis1D(action,inputDevice.getAxis1D(id));
                        break;
                    case AXIS_2D :
                        state.setAxis2D(action,inputDevice.getAxis2D(id).x, inputDevice.getAxis2D(id).y);
                        break;
                    case AXIS_1DDel:
                        state.setAxis1DDelta(action,inputDevice.getAxis1DDelta(id));
                        break;
                    case AXIS_2DDel:
                        state.setAxis2DDelta(action,inputDevice.getAxis2DDelta(id).x, inputDevice.getAxis2DDelta(id).y);
                        break;
                    default :  throw new RuntimeException("Unsupported action type");
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
    public boolean isActionDown(Action action){
        return state.isDown(action);
    }

    /**
     *  Did the action trigger once
     * @param action the action want to detect
     * @return whether the action is triggered once
     */
    public boolean isActionPressed(Action action){
        return state.isPressed(action);
    }

    /**
     *  Does the action slide
     * @param action the action want to detect
     * @return the value of sliding in current frame
     */
    public float getActionAxis1D(Action action){
        return state.getAxis1D(action);
    }

    /**
     *  Does the action slide in 2 dimensions
     * @param action the action want to detect
     * @return the value of sliding in current frame
     */
    public Vector2f getActionAxis2D(Action action){
        return state.getAxis2D(action);
    }

    /**
     *  Does the action slide(derivative)
     * @param action the action want to detect
     * @return the value of sliding in current frame
     */
    public float getActionAxis1DDelta(Action action){
        return state.getAxis1DDelta(action);
    }

    /**
     *  Does the action slide in 2 dimensions(derivative)
     * @param action the action want to detect
     * @return the value of sliding in current frame
     */
    public Vector2f getActionAxis2DDelta(Action action){
        return state.getAxis2DDelta(action);
    }

}

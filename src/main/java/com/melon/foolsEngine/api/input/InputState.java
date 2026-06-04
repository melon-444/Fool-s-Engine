package com.melon.foolsEngine.api.input;

import org.joml.Vector2f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class InputState {

    private final Map<Action, Boolean> Down = new HashMap<>();
    private final Map<Action, Boolean> Pressed = new HashMap<>();
    private final Map<Action, Float> Axis1D = new HashMap<>();
    private final Map<Action, Vector2f> Axis2D = new HashMap<>();
    private final Map<Action, Float> Axis1DDelta = new HashMap<>();
    private final Map<Action, Vector2f> Axis2DDelta = new HashMap<>();

    private final Map<Action, Boolean> DownLast = new HashMap<>();
    private final Map<Action, Boolean> PressedLast = new HashMap<>();
    private final Map<Action, Float> Axis1DLast = new HashMap<>();
    private final Map<Action, Vector2f> Axis2DLast = new HashMap<>();


    private final Set<Action> SignalFlagCache = new HashSet<>();

    public void clearSignalCache() {
        SignalFlagCache.clear();
        Pressed.clear();
    }

    public void setDown(Action action, boolean down){
        if(!SignalFlagCache.contains(action)) {
            this.DownLast.put(action,Down.getOrDefault(action, false));
            this.Down.put(action, down);
            SignalFlagCache.add(action);
        }else
            this.Down.put(action, getOrDefault(Down, action, false)||down);

    }
    public void setPressed(Action action, boolean pressed){
        if(!SignalFlagCache.contains(action)) {
            this.PressedLast.put(action,Pressed.getOrDefault(action, false));
            this.Pressed.put(action, pressed);
            SignalFlagCache.add(action);
        }else
            this.Pressed.put(action, getOrDefault(Pressed, action, false)||pressed);
    }
    public void setAxis1D(Action action, float y){
        if(!SignalFlagCache.contains(action)) {
            this.Axis1DLast.put(action, Axis1D.getOrDefault(action, 0f));
            this.Axis1D.put(action, y);
            SignalFlagCache.add(action);
        }else
            this.Axis1D.put(action, getOrDefault(Axis1D, action, 0f)+ y);
    }
    public void setAxis2D(Action action, float x, float y){
        if(!SignalFlagCache.contains(action)) {
            this.Axis2DLast.put(action, Axis2D.getOrDefault(action, null));
            this.Axis2D.put(action,new Vector2f(x, y));
            SignalFlagCache.add(action);
        }else
            this.Axis2D.put(action, getOrDefault(Axis2D, action, new Vector2f(0,0)).add(x,y));
    }

    public void setAxis1DDelta(Action action, float y){
        if(!SignalFlagCache.contains(action)) {
            this.Axis1DDelta.put(action,y);
            SignalFlagCache.add(action);
        }else
            this.Axis1DDelta.put(action,getOrDefault(Axis1DDelta, action, 0f)+y);
    }

    public void setAxis2DDelta(Action action,float x, float y){
        if(!SignalFlagCache.contains(action)) {
            this.Axis2DDelta.put(action,new  Vector2f(x,y));
            SignalFlagCache.add(action);
        }else
            this.Axis2DDelta.put(action,getOrDefault(Axis2DDelta, action, new Vector2f(0,0)).add(new Vector2f(x,y)));
    }

    private static <T> T getOrDefault(Map<Action, T> map, Action action, T defaultValue) {
        T val = map.get(action);
        return val != null ? val : defaultValue;
    }

    public boolean isDown(Action action){
        return getOrDefault(Down, action, false);
    }
    public boolean isPressed(Action action){
        return Pressed.getOrDefault(action,false);
    }
    public float getAxis1D(Action action){
        return Axis1D.getOrDefault(action,0.0f);
    }
    public Vector2f getAxis2D(Action action){
        return Axis2D.getOrDefault(action,new Vector2f(0,0));
    }
    public float getAxis1DDelta(Action action){
        return Axis1DDelta.getOrDefault(action,0.0f);
    }
    public Vector2f getAxis2DDelta(Action action){
        return Axis2DDelta.getOrDefault(action,new Vector2f(0,0));
    }

    public boolean isDownLast(Action action){
        return DownLast.getOrDefault(action,false);
    }
    public boolean isPressedLast(Action action){
        return PressedLast.getOrDefault(action,false);
    }
    public float getAxis1DLast(Action action){
        return Axis1DLast.getOrDefault(action,0.0f);
    }
    public Vector2f getAxis2DLast(Action action){
        return Axis2DLast.getOrDefault(action,new Vector2f(0,0));
        }
}
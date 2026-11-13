package com.melon.foolsEngine.api.rendering.ui;

import java.util.Objects;

/** Tracks primary-button interaction against the most recently presented UI. */
public final class UiInteraction {
    static final Object BLOCKED = new Object();
    private Object hovered;
    private Object pressed;
    private Object clicked;
    private boolean down;

    /**
     * Update once after polling input. Coordinates are top-left framebuffer pixels.
     * Pass focused=false when the window loses focus or the pointer is unavailable.
     */
    public void update(UiDrawList frame, float x, float y, boolean primaryDown, boolean focused) {
        clicked = null;
        Object hit = focused ? frame.targetAt(x, y) : null;
        hovered = hit == BLOCKED ? null : hit;
        if (!focused || (pressed != null && !frame.presented(pressed))) {
            pressed = null;
        }
        if (focused && primaryDown && !down) {
            pressed = hovered;
        } else if (!primaryDown && down) {
            if (pressed != null && pressed.equals(hovered)) {
                clicked = pressed;
            }
            pressed = null;
        }
        down = primaryDown;
    }

    public Object hit() { return hovered; }
    public boolean isHovered(Object id) { return id != null && Objects.equals(hovered, id); }
    public boolean isPressed(Object id) { return id != null && Objects.equals(pressed, id); }
    public boolean isClicked(Object id) { return id != null && Objects.equals(clicked, id); }
}

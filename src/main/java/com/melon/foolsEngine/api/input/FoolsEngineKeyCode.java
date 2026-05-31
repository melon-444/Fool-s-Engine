package com.melon.foolsEngine.api.input;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

public enum FoolsEngineKeyCode {

    // --- Keyboard ---
    W(GLFW_KEY_W),
    A(GLFW_KEY_A),
    S(GLFW_KEY_S),
    D(GLFW_KEY_D),

    Q(GLFW_KEY_Q),
    E(GLFW_KEY_E),
    R(GLFW_KEY_R),
    F(GLFW_KEY_F),

    SPACE(GLFW_KEY_SPACE),
    LEFT_SHIFT(GLFW_KEY_LEFT_SHIFT),
    LEFT_CTRL(GLFW_KEY_LEFT_CONTROL),

    ESC(GLFW_KEY_ESCAPE),
    ENTER(GLFW_KEY_ENTER),
    TAB(GLFW_KEY_TAB),

    UP(GLFW_KEY_UP),
    DOWN(GLFW_KEY_DOWN),
    LEFT(GLFW_KEY_LEFT),
    RIGHT(GLFW_KEY_RIGHT),

    // --- Mouse ---
    MOUSE_LEFT(GLFW_MOUSE_BUTTON_LEFT),
    MOUSE_RIGHT(GLFW_MOUSE_BUTTON_RIGHT),
    MOUSE_MIDDLE(GLFW_MOUSE_BUTTON_MIDDLE),

    CURSOR(GLFW_CURSOR);

    private final int id;

    FoolsEngineKeyCode(int id) {
        this.id = id;
    }

    private static final Map<Integer, FoolsEngineKeyCode> ID_MAP = new HashMap<>();
    public int getId() {
        return this.id;
    }

    static {
        for (FoolsEngineKeyCode key : values()) {
            ID_MAP.put(key.id, key);
        }
    }

    public static FoolsEngineKeyCode fromId(int id) {
        return ID_MAP.get(id);
    }

}

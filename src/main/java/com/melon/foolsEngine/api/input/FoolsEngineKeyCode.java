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

import java.util.HashMap;
import java.util.Map;

//import static org.lwjgl.glfw.GLFW.*;

public enum FoolsEngineKeyCode {

    // --- Letters ---
    A(1),//GLFW_KEY_A
    B(2),//GLFW_KEY_B
    C(3),//GLFW_KEY_C
    D(4),//GLFW_KEY_D
    E(5),//GLFW_KEY_E
    F(6),//GLFW_KEY_F
    G(7),//GLFW_KEY_G
    H(8),//GLFW_KEY_H
    I(9),//GLFW_KEY_I
    J(10),//GLFW_KEY_J
    K(11),//GLFW_KEY_K
    L(12),//GLFW_KEY_L
    M(13),//GLFW_KEY_M
    N(14),//GLFW_KEY_N
    O(15),//GLFW_KEY_O
    P(16),//GLFW_KEY_P
    Q(17),//GLFW_KEY_Q
    R(18),//GLFW_KEY_R
    S(19),//GLFW_KEY_S
    T(20),//GLFW_KEY_T
    U(21),//GLFW_KEY_U
    V(22),//GLFW_KEY_V
    W(23),//GLFW_KEY_W
    X(24),//GLFW_KEY_X
    Y(25),//GLFW_KEY_Y
    Z(26),//GLFW_KEY_Z

    // --- Numbers ---
    NUM_0(27),//GLFW_KEY_0
    NUM_1(28),//GLFW_KEY_1
    NUM_2(29),//GLFW_KEY_2
    NUM_3(30),//GLFW_KEY_3
    NUM_4(31),//GLFW_KEY_4
    NUM_5(32),//GLFW_KEY_5
    NUM_6(33),//GLFW_KEY_6
    NUM_7(34),//GLFW_KEY_7
    NUM_8(35),//GLFW_KEY_8
    NUM_9(36),//GLFW_KEY_9

    // --- Function keys ---
    F1(37),//GLFW_KEY_F1
    F2(38),//GLFW_KEY_F2
    F3(39),//GLFW_KEY_F3
    F4(40),//GLFW_KEY_F4
    F5(41),//GLFW_KEY_F5
    F6(42),//GLFW_KEY_F6
    F7(43),//GLFW_KEY_F7
    F8(44),//GLFW_KEY_F8
    F9(45),//GLFW_KEY_F9
    F10(46),//GLFW_KEY_F10
    F11(47),//GLFW_KEY_F11
    F12(48),//GLFW_KEY_F12
    F13(49),//GLFW_KEY_F13
    F14(50),//GLFW_KEY_F14
    F15(51),//GLFW_KEY_F15
    F16(52),//GLFW_KEY_F16
    F17(53),//GLFW_KEY_F17
    F18(54),//GLFW_KEY_F18
    F19(55),//GLFW_KEY_F19
    F20(56),//GLFW_KEY_F20
    F21(57),//GLFW_KEY_F21
    F22(58),//GLFW_KEY_F22
    F23(59),//GLFW_KEY_F23
    F24(60),//GLFW_KEY_F24
    F25(61),//GLFW_KEY_F25

    // --- Navigation ---
    UP(62),//GLFW_KEY_UP
    DOWN(63),//GLFW_KEY_DOWN
    LEFT(64),//GLFW_KEY_LEFT
    RIGHT(65),//GLFW_KEY_RIGHT

    // --- Modifiers ---
    SPACE(66),//GLFW_KEY_SPACE
    LEFT_SHIFT(67),//GLFW_KEY_LEFT_SHIFT
    LEFT_CTRL(68),//GLFW_KEY_LEFT_CONTROL
    LEFT_ALT(69),//GLFW_KEY_LEFT_ALT
    RIGHT_SHIFT(70),//GLFW_KEY_RIGHT_SHIFT
    RIGHT_CTRL(71),//GLFW_KEY_RIGHT_CONTROL
    RIGHT_ALT(72),//GLFW_KEY_RIGHT_ALT
    LEFT_SUPER(73),//GLFW_KEY_LEFT_SUPER
    RIGHT_SUPER(74),//GLFW_KEY_RIGHT_SUPER

    // --- Special ---
    ESC(75),//GLFW_KEY_ESCAPE
    ENTER(76),//GLFW_KEY_ENTER
    TAB(77),//GLFW_KEY_TAB
    BACKSPACE(78),//GLFW_KEY_BACKSPACE
    INSERT(79),//GLFW_KEY_INSERT
    DELETE(80),//GLFW_KEY_DELETE
    HOME(81),//GLFW_KEY_HOME
    END(82),//GLFW_KEY_END
    PAGE_UP(83),//GLFW_KEY_PAGE_UP
    PAGE_DOWN(84),//GLFW_KEY_PAGE_DOWN

    // --- Lock keys ---
    CAPS_LOCK(85),//GLFW_KEY_CAPS_LOCK
    NUM_LOCK(86),//GLFW_KEY_NUM_LOCK
    SCROLL_LOCK(87),//GLFW_KEY_SCROLL_LOCK
    PRINT_SCREEN(88),//GLFW_KEY_PRINT_SCREEN
    PAUSE(89),//GLFW_KEY_PAUSE

    // --- Symbols ---
    APOSTROPHE(90),//GLFW_KEY_APOSTROPHE
    COMMA(91),//GLFW_KEY_COMMA
    MINUS(92),//GLFW_KEY_MINUS
    PERIOD(93),//GLFW_KEY_PERIOD
    SLASH(94),//GLFW_KEY_SLASH
    SEMICOLON(95),//GLFW_KEY_SEMICOLON
    EQUAL(96),//GLFW_KEY_EQUAL
    LEFT_BRACKET(97),//GLFW_KEY_LEFT_BRACKET
    BACKSLASH(98),//GLFW_KEY_BACKSLASH
    RIGHT_BRACKET(99),//GLFW_KEY_RIGHT_BRACKET
    GRAVE_ACCENT(100),//GLFW_KEY_GRAVE_ACCENT

    // --- World keys ---
    WORLD_1(101),//GLFW_KEY_WORLD_1
    WORLD_2(102),//GLFW_KEY_WORLD_2
    MENU(103),//GLFW_KEY_MENU

    // --- Numpad ---
    KP_0(104),//GLFW_KEY_KP_0
    KP_1(105),//GLFW_KEY_KP_1
    KP_2(106),//GLFW_KEY_KP_2
    KP_3(107),//GLFW_KEY_KP_3
    KP_4(108),//GLFW_KEY_KP_4
    KP_5(109),//GLFW_KEY_KP_5
    KP_6(110),//GLFW_KEY_KP_6
    KP_7(111),//GLFW_KEY_KP_7
    KP_8(112),//GLFW_KEY_KP_8
    KP_9(113),//GLFW_KEY_KP_9
    KP_DECIMAL(114),//GLFW_KEY_KP_DECIMAL
    KP_DIVIDE(115),//GLFW_KEY_KP_DIVIDE
    KP_MULTIPLY(116),//GLFW_KEY_KP_MULTIPLY
    KP_SUBTRACT(117),//GLFW_KEY_KP_SUBTRACT
    KP_ADD(118),//GLFW_KEY_KP_ADD
    KP_ENTER(119),//GLFW_KEY_KP_ENTER
    KP_EQUAL(120),//GLFW_KEY_KP_EQUAL

    // --- Mouse ---
    MOUSE_LEFT(121),//GLFW_MOUSE_BUTTON_LEFT
    MOUSE_RIGHT(122),//GLFW_MOUSE_BUTTON_RIGHT
    MOUSE_MIDDLE(123),//GLFW_MOUSE_BUTTON_MIDDLE
    MOUSE_4(124),//GLFW_MOUSE_BUTTON_4
    MOUSE_5(125),//GLFW_MOUSE_BUTTON_5
    MOUSE_6(126),//GLFW_MOUSE_BUTTON_6
    MOUSE_7(127),//GLFW_MOUSE_BUTTON_7
    MOUSE_8(128),//GLFW_MOUSE_BUTTON_8

    CURSOR(129),//GLFW_CURSOR
    NULL(-1);//-1

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

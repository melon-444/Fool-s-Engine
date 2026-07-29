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
package com.melon.foolsEngine.util;

/**
 * Cursor behavior modes for a {@link com.melon.foolsEngine.api.windows.Window}.
 * <p>
 * Set via {@link com.melon.foolsEngine.api.windows.Window#setCursorMode(CursorMode)}:
 * <pre>{@code
 *   win.setCursorMode(CursorMode.DISABLED); // FPS-style unlimited cursor
 *   win.setCursorMode(CursorMode.HIDDEN);   // hidden but screen-constrained
 *   win.setCursorMode(CursorMode.NORMAL);   // default OS cursor
 * }</pre>
 */
public enum CursorMode {
    /** Normal OS cursor, visible and screen-constrained */
    NORMAL,
    /** Cursor is hidden but still constrained to the window */
    HIDDEN,
    /** Cursor is hidden and provides unlimited virtual movement (FPS camera style) */
    DISABLED
}

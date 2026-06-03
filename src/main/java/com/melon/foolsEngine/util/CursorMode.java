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

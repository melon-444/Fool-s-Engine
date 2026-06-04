package com.melon.foolsEngine.util;

/**
 * Wrapping strategy used by {@link LoadMode#CROP_WRAP}
 * when the source image is smaller than the target texture-array dimensions.
 */
public enum WrapMode {
    /** Fill uncovered pixels with transparent black RGBA(0,0,0,0). */
    CLAMP_TO_BORDER,

    /** Tile the image repeatedly to fill the area. */
    REPEAT,

    /** Tile the image with mirror-flipping at every tile boundary. */
    MIRRORED_REPEAT,

    /** Extend the edge pixels outward to fill the area. */
    CLAMP_TO_EDGE
}

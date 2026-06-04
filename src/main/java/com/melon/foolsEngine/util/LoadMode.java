package com.melon.foolsEngine.util;

import com.melon.foolsEngine.api.rendering.resource.TextureManager;

/**
 * Controls how {@link TextureManager} handles images whose dimensions
 * differ from the target texture-array size.
 */
public enum LoadMode {
    /** Scale the image to fit the array dimensions (nearest-neighbor). Default. */
    STRETCH,

    /**
     * If the image is larger: crop the top-left region to fit.
     * If smaller: wrap the remaining area using the configured {@link WrapMode}.
     */
    CROP_WRAP,

    /** Reject images whose dimensions differ from the array. */
    STRICT
}

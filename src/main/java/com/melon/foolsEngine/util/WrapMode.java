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

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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ImageFormatDetector {

    private ImageFormatDetector() {}

    public enum Format {
        PNG, JPEG, BMP, GIF, TIFF, WEBP, TGA, PSD, HDR, UNKNOWN
    }

    public static Format detect(byte[] header) {
        if (header.length < 4) return Format.UNKNOWN;
        int b0 = header[0] & 0xFF;
        int b1 = header[1] & 0xFF;
        int b2 = header[2] & 0xFF;
        int b3 = header[3] & 0xFF;

        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return Format.PNG;
        if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return Format.JPEG;
        if (b0 == 0x42 && b1 == 0x4D) return Format.BMP;
        if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38) return Format.GIF;
        if (b0 == 0x4D && b1 == 0x4D && b2 == 0x00 && b3 == 0x2A) return Format.TIFF;
        if (b0 == 0x49 && b1 == 0x49 && b2 == 0x2A && b3 == 0x00) return Format.TIFF;
        if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46) {
            if (header.length >= 12) {
                int b8  = header[8]  & 0xFF;
                int b9  = header[9]  & 0xFF;
                int b10 = header[10] & 0xFF;
                int b11 = header[11] & 0xFF;
                if (b8 == 0x57 && b9 == 0x45 && b10 == 0x42 && b11 == 0x50) return Format.WEBP;
            }
        }
        if (header.length >= 18) {
            int b16 = header[16] & 0xFF;
            int b17 = header[17] & 0xFF;
            if (b0 == 0 && b1 == 0 && b3 == 0 && b16 == 0 && b17 == 0) return Format.TGA;
            if (b0 == 0 && b1 == 0 && b3 <= 3 && b2 >= 1 && b2 <= 3) return Format.TGA;
        }
        if (b0 == 0x38 && b1 == 0x42 && b2 == 0x50 && b3 == 0x53) return Format.PSD;
        if (b0 == 0x23 && b1 == 0x3F) return Format.HDR;

        return Format.UNKNOWN;
    }

    public static Format detect(Path path) throws IOException {
        byte[] header = new byte[18];
        try (InputStream is = Files.newInputStream(path)) {
            int read = is.read(header);
            if (read < 4) return Format.UNKNOWN;
            if (read < 18) {
                byte[] trimmed = new byte[read];
                System.arraycopy(header, 0, trimmed, 0, read);
                return detect(trimmed);
            }
        }
        return detect(header);
    }

    public static boolean isStbSupported(Format format) {
        return switch (format) {
            case PNG, JPEG, BMP, GIF, TGA, PSD, HDR -> true;
            case TIFF, WEBP, UNKNOWN -> false;
        };
    }
}

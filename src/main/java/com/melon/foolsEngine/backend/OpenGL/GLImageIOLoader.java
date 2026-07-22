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

package com.melon.foolsEngine.backend.OpenGL;

import org.lwjgl.system.MemoryUtil;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

class GLImageIOLoader {

    private GLImageIOLoader() {}

    static ImageIOResult load(byte[] fileData) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(fileData));
            if (img == null) return null;

            int w = img.getWidth();
            int h = img.getHeight();

            BufferedImage rgba = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = rgba.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();

            int[] pixels = new int[w * h];
            rgba.getRGB(0, 0, w, h, pixels, 0, w);

            ByteBuffer buf = MemoryUtil.memAlloc(w * h * 4);
            for (int y = h - 1; y >= 0; y--) {
                int rowOffset = y * w;
                for (int x = 0; x < w; x++) {
                    int pixel = pixels[rowOffset + x];
                    buf.put((byte) ((pixel >> 16) & 0xFF));
                    buf.put((byte) ((pixel >> 8) & 0xFF));
                    buf.put((byte) (pixel & 0xFF));
                    buf.put((byte) ((pixel >> 24) & 0xFF));
                }
            }
            ((Buffer) buf).flip();
            return new ImageIOResult(buf, w, h);
        } catch (IOException e) {
            return null;
        }
    }

    record ImageIOResult(ByteBuffer pixels, int width, int height) {}
}

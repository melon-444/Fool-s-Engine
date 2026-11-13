package com.melon.foolsEngine.api.rendering.ui;

import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Frame-local UI commands. Rectangles use top-left framebuffer pixel coordinates. */
public final class UiDrawList {
    public record WorldAnchor(float x, float y, float z) {
        public WorldAnchor {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) {
                throw new IllegalArgumentException("UI anchor must be finite");
            }
        }
    }

    public record Color(float r, float g, float b, float a) {
        public static final Color WHITE = new Color(1, 1, 1, 1);

        public Color {
            if (!valid(r) || !valid(g) || !valid(b) || !valid(a)) {
                throw new IllegalArgumentException("UI colors must be between 0 and 1");
            }
        }

        private static boolean valid(float value) {
            return value >= 0 && value <= 1;
        }
    }

    /**
     * A colored or textured quad. A world anchor projects to a pixel origin;
     * x and y then act as pixel offsets from that origin.
     */
    public record Quad(float x, float y, float width, float height, Color color,
                       Texture texture, int layer, WorldAnchor anchor) {
        public Quad {
            Objects.requireNonNull(color, "color");
            if (!Float.isFinite(x) || !Float.isFinite(y) ||
                    !Float.isFinite(width) || !Float.isFinite(height) || width < 0 || height < 0) {
                throw new IllegalArgumentException("Invalid UI rectangle");
            }
        }

        public Quad(float x, float y, float width, float height, Color color, int layer) {
            this(x, y, width, height, color, null, layer, null);
        }

        public Quad at(WorldAnchor position) {
            return new Quad(x, y, width, height, color, texture, layer, Objects.requireNonNull(position));
        }
    }

    private final List<Quad> commands = new ArrayList<>();

    public void submit(Quad quad) {
        commands.add(Objects.requireNonNull(quad, "quad"));
    }

    public void rectangle(float x, float y, float width, float height, Color color, int layer) {
        submit(new Quad(x, y, width, height, color, layer));
    }

    public void image(float x, float y, float width, float height, Texture texture, int layer) {
        submit(new Quad(x, y, width, height, Color.WHITE, Objects.requireNonNull(texture), layer, null));
    }

    /** Ascending layer order, preserving submission order for ties. */
    public List<Quad> orderedCommands() {
        var sorted = new ArrayList<>(commands);
        sorted.sort(Comparator.comparingInt(Quad::layer));
        return List.copyOf(sorted);
    }

    public boolean isEmpty() { return commands.isEmpty(); }

    public void clear() { commands.clear(); }
}

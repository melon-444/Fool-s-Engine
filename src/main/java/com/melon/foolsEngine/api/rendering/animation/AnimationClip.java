package com.melon.foolsEngine.api.rendering.animation;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

/** Joint tracks indexed by skeleton node, with all timestamps in seconds. */
public record AnimationClip(String name, double durationSeconds, Map<Integer, Track> tracks) {
    public AnimationClip {
        if (!Double.isFinite(durationSeconds) || durationSeconds < 0) {
            throw new IllegalArgumentException("Invalid animation duration");
        }
        tracks = Map.copyOf(tracks);
    }

    public record Track(List<VectorKey> positions, List<RotationKey> rotations, List<VectorKey> scales) {
        public Track {
            positions = List.copyOf(positions);
            rotations = List.copyOf(rotations);
            scales = List.copyOf(scales);
        }
    }

    public record VectorKey(double time, float x, float y, float z) {
        Vector3f set(Vector3f out) {
            return out.set(x, y, z);
        }
    }

    public record RotationKey(double time, float x, float y, float z, float w) {
        Quaternionf set(Quaternionf out) {
            return out.set(x, y, z, w).normalize();
        }
    }
}

package com.melon.foolsEngine.util;

import com.melon.foolsEngine.api.rendering.animation.AnimationClip;

import java.util.HashMap;
import java.util.List;

/** Workaround for Assimp's extra Euler rotation keys in frame-baked FBX clips. */
public final class FbxRotationRepair {
    private static final double FRAME_TOLERANCE = 1e-4;

    private FbxRotationRepair() {}

    /**
     * Keeps the authored rotation samples and lets the player interpolate between
     * them. Use only when the source FBX was baked at the given rate and all authored
     * rotations are on that grid. Frame zero is the start of the imported clip.
     * Translation, scale, and the on-grid rotations are preserved.
     */
    public static AnimationClip restoreBakedFrames(AnimationClip clip, double framesPerSecond) {
        if (!Double.isFinite(framesPerSecond) || framesPerSecond <= 0) {
            throw new IllegalArgumentException("Bake frame rate must be positive and finite");
        }
        var tracks = new HashMap<Integer, AnimationClip.Track>();
        boolean changed = false;
        for (var entry : clip.tracks().entrySet()) {
            var track = entry.getValue();
            var rotations = track.rotations();
            if (rotations.size() > 1) {
                var baked = rotations.stream().filter(key -> onFrame(key.time(), framesPerSecond)).toList();
                if (baked.size() != rotations.size()) {
                    requireBakedRange(rotations, baked, framesPerSecond, entry.getKey());
                    track = new AnimationClip.Track(track.positions(), baked, track.scales());
                    changed = true;
                }
            }
            tracks.put(entry.getKey(), track);
        }
        return changed ? new AnimationClip(clip.name(), clip.durationSeconds(), tracks) : clip;
    }

    private static boolean onFrame(double time, double rate) {
        double frame = time * rate;
        return Math.abs(frame - Math.rint(frame)) <= FRAME_TOLERANCE;
    }

    private static void requireBakedRange(List<AnimationClip.RotationKey> original,
                                          List<AnimationClip.RotationKey> baked, double rate, int joint) {
        if (baked.size() >= 2 && baked.getFirst() == original.getFirst() && baked.getLast() == original.getLast()) {
            boolean complete = true;
            for (int i = 1; i < baked.size(); i++) {
                if (Math.rint(baked.get(i).time() * rate) - Math.rint(baked.get(i - 1).time() * rate) != 1) {
                    complete = false;
                    break;
                }
            }
            if (complete) return;
        }
        throw new IllegalArgumentException("Rotation track " + joint
                + " does not have consecutive baked samples at " + rate + " FPS");
    }
}

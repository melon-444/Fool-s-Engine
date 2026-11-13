package com.melon.foolsEngine.api.rendering.animation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Copies local tracks between compatible rigs without transferring scene placement. */
public final class AnimationBinding {
    private AnimationBinding() {}

    public static AnimationClip bind(AnimationClip clip, Skeleton source, Skeleton target) {
        return bind(clip, source, target, Set.of());
    }

    /**
     * Remaps tracks by unique node name. Unanimated scene roots may have different
     * transforms, since the target model retains its own placement.
     * An ignored name must be absent from the target. A dropped ancestor of a
     * bound track still causes a hierarchy mismatch.
     */
    public static AnimationClip bind(AnimationClip clip, Skeleton source, Skeleton target,
                                     Set<String> ignoredTracks) {
        var sourceNames = index(source);
        var targetNames = index(target);
        var tracks = new HashMap<Integer, AnimationClip.Track>();
        for (var entry : clip.tracks().entrySet()) {
            int sourceId = entry.getKey();
            if (sourceId < 0 || sourceId >= source.joints().size()) {
                throw new IllegalArgumentException("Unknown source track index: " + sourceId);
            }
            String name = source.joints().get(sourceId).name();
            int targetId = targetNames.getOrDefault(name, -2);
            if (targetId == -2 && ignoredTracks.contains(name)) continue;
            if (targetId < 0 || sourceNames.get(name) != sourceId) {
                throw new IllegalArgumentException("Missing or ambiguous animation node: " + name);
            }
            checkHierarchy(clip, source, target, sourceId, targetId);
            tracks.put(targetId, entry.getValue());
        }
        if (tracks.isEmpty()) throw new IllegalArgumentException("No compatible animation tracks in " + clip.name());
        return new AnimationClip(clip.name(), clip.durationSeconds(), tracks);
    }

    private static void checkHierarchy(AnimationClip clip, Skeleton source, Skeleton target, int from, int to) {
        while (from >= 0 && to >= 0) {
            var sourceJoint = source.joints().get(from);
            var targetJoint = target.joints().get(to);
            boolean sceneRoot = sourceJoint.parent() == -1 && targetJoint.parent() == -1
                    && !clip.tracks().containsKey(from);
            if (!sceneRoot && (!sourceJoint.name().equals(targetJoint.name())
                    || !sourceJoint.bindLocal().equals(targetJoint.bindLocal(), 0.001f))) {
                throw new IllegalArgumentException("Hierarchy or local bind pose differs at " + sourceJoint.name());
            }
            from = sourceJoint.parent();
            to = targetJoint.parent();
        }
        if (from != to) throw new IllegalArgumentException("Animation hierarchy depths differ");
    }

    private static Map<String, Integer> index(Skeleton skeleton) {
        var names = new HashMap<String, Integer>();
        for (int i = 0; i < skeleton.joints().size(); i++) {
            names.merge(skeleton.joints().get(i).name(), i, (first, second) -> -1);
        }
        return names;
    }
}

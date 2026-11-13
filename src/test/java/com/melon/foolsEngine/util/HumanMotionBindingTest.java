package com.melon.foolsEngine.util;

import com.melon.foolsEngine.api.rendering.animation.AnimationBinding;
import com.melon.foolsEngine.api.rendering.animation.AnimationClip;
import com.melon.foolsEngine.api.rendering.animation.AnimationPlayer;
import com.melon.foolsEngine.api.rendering.animation.Skeleton;
import com.melon.foolsEngine.core.ECS.basicComponents.SkeletalAnimationComponent;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class HumanMotionBindingTest {
    private static final Path LIBRARY = Path.of("src/test/resources/hmanimation");

    @Test
    void rejectsSameNameWithDifferentBindPose() {
        var root = new Skeleton.Joint("scene", -1, new Matrix4f());
        var source = new Skeleton(List.of(root, new Skeleton.Joint("hip", 0, new Matrix4f())));
        var target = new Skeleton(List.of(root, new Skeleton.Joint("hip", 0, new Matrix4f().translation(0, 2, 0))));
        var track = new AnimationClip.Track(List.of(new AnimationClip.VectorKey(0, 0, 0, 0)), List.of(), List.of());
        var clip = new AnimationClip("motion", 1, Map.of(1, track));
        assertThrows(IllegalArgumentException.class, () -> AnimationBinding.bind(clip, source, target));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Female", "Male"})
    void loadsSeparateSprintForCharacter(String variant) {
        String character = "Human" + variant.charAt(0);
        Path modelFile = LIBRARY.resolve("Models/" + character + "_Model.fbx");
        Path sprintFile = LIBRARY.resolve("Animations/" + variant + "/Movement/Sprint/"
                + character + "@Sprint01_Forward.fbx");
        assumeTrue(Files.isRegularFile(modelFile) && Files.isRegularFile(sprintFile));
        var model = MeshImporter.load(modelFile);
        var part = model.parts().stream().filter(p -> p.skin() != null).findFirst().orElseThrow();
        var sprint = MeshImporter.loadAnimation(sprintFile);
        var sourceClip = sprint.clips().getFirst();
        var failure = assertThrows(IllegalArgumentException.class,
                () -> AnimationBinding.bind(sourceClip, sprint.skeleton(), model.skeleton()));
        assertTrue(failure.getMessage().contains("B-spineProxy"), failure.getMessage());
        var repaired = FbxRotationRepair.restoreBakedFrames(sourceClip, 30);
        if (variant.equals("Female")) {
            int shoulder = joint(new AnimationPlayer(sprint.skeleton()), "B-shoulder.L");
            var original = sourceClip.tracks().get(shoulder).rotations();
            var cleaned = repaired.tracks().get(shoulder).rotations();
            assertEquals(17, cleaned.size());
            assertEquals(original.stream().filter(k -> Math.abs(k.time() * 30 - Math.rint(k.time() * 30)) < 1e-4)
                    .toList(), cleaned);
            assertTrue(largestRotationStep(cleaned) < 30);
        }
        var bound = AnimationBinding.bind(repaired, sprint.skeleton(), model.skeleton(), Set.of("B-spineProxy"));
        assertEquals(sourceClip.tracks().size() - 1, bound.tracks().size());

        var sourcePlayer = new AnimationPlayer(sprint.skeleton());
        var targetPlayer = new AnimationPlayer(model.skeleton());
        var animatedPart = new SkeletalAnimationComponent(targetPlayer, part.skin(), part.node());
        sourcePlayer.play(repaired, false);
        targetPlayer.play(bound, false);
        float[] first = animatedPart.deformedVertices();
        sourcePlayer.advance(bound.durationSeconds() / 3);
        targetPlayer.advance(bound.durationSeconds() / 3);
        float[] posed = animatedPart.deformedVertices();
        assertFalse(Arrays.equals(first, posed));
        for (float v : posed) assertTrue(Float.isFinite(v));
        for (String name : Set.of("B-chest", "B-head", "B-hand.L", "B-foot.R")) {
            var sourcePose = sourcePlayer.jointTransform(joint(sourcePlayer, name), new Matrix4f());
            var targetPose = targetPlayer.jointTransform(joint(targetPlayer, name), new Matrix4f());
            // The FBX scene roots differ in orientation. Compare poses in each rig's scene basis.
            sourcePlayer.jointTransform(0, new Matrix4f()).invert().mul(sourcePose, sourcePose);
            targetPlayer.jointTransform(0, new Matrix4f()).invert().mul(targetPose, targetPose);
            assertTrue(sourcePose.equals(targetPose, 0.001f), name);
        }

        targetPlayer.play(bound, false);
        float[] previous = animatedPart.deformedVertices();
        float maxStep = 0;
        for (int frame = 1; frame <= 480; frame++) {
            targetPlayer.advance(bound.durationSeconds() / 480);
            float[] current = animatedPart.deformedVertices();
            for (int i = 0; i < current.length; i += part.mesh().layout().stride()) {
                float dx = current[i] - previous[i];
                float dy = current[i + 1] - previous[i + 1];
                float dz = current[i + 2] - previous[i + 2];
                maxStep = Math.max(maxStep, dx * dx + dy * dy + dz * dz);
            }
            previous = current;
        }
        assertTrue(maxStep < 16, "Largest vertex step: " + Math.sqrt(maxStep));
    }

    private static double largestRotationStep(List<AnimationClip.RotationKey> keys) {
        double max = 0;
        for (int i = 1; i < keys.size(); i++) {
            var a = keys.get(i - 1);
            var b = keys.get(i);
            double dot = Math.abs(a.x() * b.x() + a.y() * b.y() + a.z() * b.z() + a.w() * b.w());
            max = Math.max(max, Math.toDegrees(2 * Math.acos(Math.min(1, dot))));
        }
        return max;
    }

    private static int joint(AnimationPlayer player, String name) {
        var joints = player.skeleton().joints();
        for (int i = 0; i < joints.size(); i++) {
            if (joints.get(i).name().equals(name)) return i;
        }
        throw new AssertionError("Missing test joint " + name);
    }
}

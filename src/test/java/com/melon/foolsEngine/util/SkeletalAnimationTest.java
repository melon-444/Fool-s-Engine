package com.melon.foolsEngine.util;

import com.melon.foolsEngine.api.rendering.animation.AnimationClip;
import com.melon.foolsEngine.api.rendering.animation.AnimationPlayer;
import com.melon.foolsEngine.api.rendering.animation.MeshSkin;
import com.melon.foolsEngine.api.rendering.animation.Skeleton;
import com.melon.foolsEngine.api.rendering.resource.MeshData;
import com.melon.foolsEngine.core.ECS.basicComponents.SkeletalAnimationComponent;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class SkeletalAnimationTest {
    @Test
    void skinsInMeshSpaceWithIndependentPlayers() {
        var skeleton = new Skeleton(List.of(
                new Skeleton.Joint("mesh", -1, new Matrix4f().translation(10, 0, 0)),
                new Skeleton.Joint("bone", 0, new Matrix4f().translation(2, 0, 0))));
        var mesh = new MeshData(new float[]{3, 0, 0, 1, 0, 0, 4, 5, 6, 0, 1, 0}, new int[0],
                new VertexLayout().add(0, 3).add(2, 3));
        var skin = new MeshSkin(skeleton, mesh,
                List.of(new MeshSkin.Bone(1, new Matrix4f().translation(-2, 0, 0))),
                List.of(List.of(new MeshSkin.Influence(0, 0.5f)), List.of()));
        var clip = new AnimationClip("turn", 2, Map.of(1, new AnimationClip.Track(
                List.of(new AnimationClip.VectorKey(0, 2, 0, 0), new AnimationClip.VectorKey(2, 6, 0, 0)),
                List.of(new AnimationClip.RotationKey(0, 0, 0, 0, 1),
                        new AnimationClip.RotationKey(2, 0, 0, 1, 0)), List.of())));
        var player = new AnimationPlayer(skeleton);
        var part = new SkeletalAnimationComponent(player, skin, 0);
        assertArrayEquals(mesh.vertices(), part.deformedVertices(), 1e-5f);
        player.play(clip, true);
        player.advance(1);
        float[] deformed = part.deformedVertices();
        assertArrayEquals(new float[]{4, 1, 0, 0, 1, 0, 4, 5, 6, 0, 1, 0}, deformed, 1e-5f);
        assertArrayEquals(mesh.vertices(), skin.deform(new AnimationPlayer(skeleton), part.meshTransform(new Matrix4f())),
                1e-5f);
        player.pause();
        player.advance(1);
        assertSame(deformed, part.deformedVertices());
        player.resume();
        player.advance(1);
        assertEquals(0, player.timeSeconds(), 1e-6);
        assertArrayEquals(mesh.vertices(), part.deformedVertices(), 1e-5f);
        player.play(clip, false);
        player.advance(3);
        assertEquals(2, player.timeSeconds(), 1e-6);
        assertFalse(player.isPlaying());
        player.stop();
        assertArrayEquals(mesh.vertices(), part.deformedVertices(), 1e-5f);
    }

    @Test
    void importsAndDeformsExternalAnimatedAsset() {
        String path = System.getenv("FOOLSENGINE_ANIMATION_ASSET");
        assumeTrue(path != null && !path.isBlank(), "Set FOOLSENGINE_ANIMATION_ASSET to an animated model");
        var model = MeshImporter.load(Path.of(path));
        var player = new AnimationPlayer(model.skeleton());
        var part = model.parts().stream().filter(p -> p.skin() != null).findFirst().orElseThrow();
        var animatedPart = new SkeletalAnimationComponent(player, part.skin(), part.node());
        var clip = model.clips().stream().filter(c -> c.durationSeconds() > 0).findFirst().orElseThrow();
        player.play(clip, false);
        float[] start = animatedPart.deformedVertices();
        player.advance(clip.durationSeconds() / 3);
        float[] deformed = animatedPart.deformedVertices();
        assertEquals(part.mesh().vertices().length, deformed.length);
        assertFalse(Arrays.equals(start, deformed));
        for (float value : deformed) assertTrue(Float.isFinite(value));
    }
}

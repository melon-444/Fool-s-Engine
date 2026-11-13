package com.melon.foolsEngine.api.rendering.animation;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Objects;
import java.util.function.ToDoubleFunction;

/** Playback state and evaluated model-space pose for one instance of a skeleton. */
public final class AnimationPlayer {
    private final Skeleton skeleton;
    private final Matrix4f[] bind;
    private final Matrix4f[] pose;
    private final Matrix4f local = new Matrix4f();
    private final Vector3f position = new Vector3f();
    private final Vector3f scale = new Vector3f();
    private final Vector3f vectorEnd = new Vector3f();
    private final Quaternionf rotation = new Quaternionf();
    private final Quaternionf rotationEnd = new Quaternionf();
    private AnimationClip clip;
    private double time;
    private boolean looping;
    private boolean playing;
    private long poseVersion;

    public AnimationPlayer(Skeleton skeleton) {
        this.skeleton = Objects.requireNonNull(skeleton);
        bind = skeleton.joints().stream().map(joint -> new Matrix4f(joint.bindLocal())).toArray(Matrix4f[]::new);
        pose = new Matrix4f[bind.length];
        for (int i = 0; i < pose.length; i++) pose[i] = new Matrix4f();
        evaluate();
    }

    public Skeleton skeleton() { return skeleton; }
    public double timeSeconds() { return time; }
    public boolean isPlaying() { return playing; }
    public long poseVersion() { return poseVersion; }

    public void play(AnimationClip clip, boolean looping) {
        Objects.requireNonNull(clip);
        for (int joint : clip.tracks().keySet()) {
            if (joint < 0 || joint >= pose.length) throw new IllegalArgumentException("Invalid track joint: " + joint);
        }
        this.clip = clip;
        this.looping = looping;
        time = 0;
        playing = clip.durationSeconds() > 0;
        evaluate();
    }

    public void pause() { playing = false; }

    public void resume() {
        playing = clip != null && clip.durationSeconds() > 0 && (looping || time < clip.durationSeconds());
    }

    public void stop() {
        clip = null;
        playing = false;
        time = 0;
        evaluate();
    }

    public void advance(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Invalid elapsed time");
        if (!playing || seconds == 0) return;
        double duration = clip.durationSeconds();
        time = looping ? (time + seconds % duration) % duration : Math.min(time + seconds, duration);
        if (!looping && time == duration) playing = false;
        evaluate();
    }

    public Matrix4f jointTransform(int joint, Matrix4f out) {
        return out.set(pose[joint]);
    }

    public Matrix4f bindTransform(int joint, Matrix4f out) {
        out.set(bind[joint]);
        for (int parent = skeleton.joints().get(joint).parent(); parent >= 0;
             parent = skeleton.joints().get(parent).parent()) {
            out.mulLocal(bind[parent]);
        }
        return out;
    }

    Matrix4fc jointTransform(int joint) { return pose[joint]; }

    private void evaluate() {
        poseVersion++;
        for (int i = 0; i < pose.length; i++) {
            var track = clip == null ? null : clip.tracks().get(i);
            Matrix4fc transform = bind[i];
            if (track != null) {
                if (track.positions().isEmpty()) bind[i].getTranslation(position);
                else sampleVector(track.positions(), position);
                if (track.scales().isEmpty()) bind[i].getScale(scale);
                else sampleVector(track.scales(), scale);
                if (track.rotations().isEmpty()) {
                    bind[i].getUnnormalizedRotation(rotation).normalize();
                } else {
                    var keys = track.rotations();
                    int previous = floor(keys, AnimationClip.RotationKey::time, time);
                    var start = keys.get(previous);
                    var end = keys.get(Math.min(previous + 1, keys.size() - 1));
                    start.set(rotation).slerp(end.set(rotationEnd), fraction(start.time(), end.time()));
                }
                transform = local.translationRotateScale(position, rotation, scale);
            }
            int parent = skeleton.joints().get(i).parent();
            if (parent < 0) pose[i].set(transform);
            else pose[parent].mul(transform, pose[i]);
        }
    }

    private void sampleVector(List<AnimationClip.VectorKey> keys, Vector3f out) {
        int previous = floor(keys, AnimationClip.VectorKey::time, time);
        var start = keys.get(previous);
        var end = keys.get(Math.min(previous + 1, keys.size() - 1));
        start.set(out).lerp(end.set(vectorEnd), fraction(start.time(), end.time()));
    }

    private float fraction(double start, double end) {
        return end <= start ? 0 : (float) Math.clamp((time - start) / (end - start), 0, 1);
    }

    private static <K> int floor(List<K> keys, ToDoubleFunction<K> timestamp, double time) {
        int low = 0;
        int high = keys.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (timestamp.applyAsDouble(keys.get(middle)) <= time) low = middle + 1;
            else high = middle;
        }
        return Math.max(0, low - 1);
    }
}

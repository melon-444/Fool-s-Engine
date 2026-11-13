package com.melon.foolsEngine.util;

import com.melon.foolsEngine.api.rendering.animation.AnimationClip;
import com.melon.foolsEngine.api.rendering.animation.MeshSkin;
import com.melon.foolsEngine.api.rendering.animation.Skeleton;
import com.melon.foolsEngine.api.rendering.resource.MeshData;
import com.melon.foolsEngine.exception.ResourceException;
import org.joml.Matrix4f;
import org.lwjgl.assimp.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.lwjgl.assimp.Assimp.*;

/** Imports CPU mesh data. Returned data does not depend on an open Assimp scene. */
public final class MeshImporter {
    private MeshImporter() {}

    /** Original Assimp metadata. Use Model.clips() for playback. */
    public record Animation(String name, double durationTicks, double ticksPerSecond, int channels) {}

    public record Part(String name, MeshData mesh, Matrix4f transform, int materialIndex,
                       List<String> boneNames, MeshSkin skin, int node) {
        public Part(String name, MeshData mesh, Matrix4f transform, int materialIndex, List<String> boneNames) {
            this(name, mesh, transform, materialIndex, boneNames, null, -1);
        }
    }

    public record Model(List<Part> parts, List<Animation> animations, Skeleton skeleton,
                        List<AnimationClip> clips) {
        public Model(List<Part> parts, List<Animation> animations) {
            this(parts, animations, new Skeleton(List.of()), List.of());
        }
    }

    /**
     * Uses the built-in OBJ loader for OBJ and Assimp for other formats such as
     * glTF, GLB, FBX and PLY. Each part retains its node-to-model transform.
     */
    public static Model load(Path path) {
        if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".obj")) {
            return new Model(List.of(new Part(path.getFileName().toString(), ObjLoader.loadMesh(path),
                    new Matrix4f(), -1, List.of())), List.of());
        }

        var scene = aiImportFile(path.toAbsolutePath().toString(), aiProcess_Triangulate
                | aiProcess_JoinIdenticalVertices | aiProcess_GenSmoothNormals | aiProcess_SortByPType);
        if (scene == null) {
            throw new ResourceException("Cannot import " + path + ": " + aiGetErrorString());
        }
        try {
            var parts = new ArrayList<Part>();
            var root = scene.mRootNode();
            var joints = new ArrayList<Skeleton.Joint>();
            var names = new HashMap<String, Integer>();
            if (root != null) collectJoints(root, -1, joints, names);
            var skeleton = new Skeleton(joints);
            if (root != null) collect(scene, root, new Matrix4f(), new Part[scene.mNumMeshes()],
                    parts, skeleton, names, 0);
            if (parts.isEmpty()) {
                throw new ResourceException("No triangle meshes in " + path);
            }
            var animations = new ArrayList<Animation>();
            var clips = new ArrayList<AnimationClip>();
            for (int i = 0; i < scene.mNumAnimations(); i++) {
                var animation = AIAnimation.create(scene.mAnimations().get(i));
                animations.add(new Animation(animation.mName().dataString(), animation.mDuration(),
                        animation.mTicksPerSecond(), animation.mNumChannels()));
                clips.add(copyAnimation(animation, names));
            }
            return new Model(List.copyOf(parts), List.copyOf(animations), skeleton, List.copyOf(clips));
        } finally {
            aiReleaseImport(scene);
        }
    }

    private static Matrix4f matrix(AIMatrix4x4 m) {
        return new Matrix4f(
                m.a1(), m.b1(), m.c1(), m.d1(),
                m.a2(), m.b2(), m.c2(), m.d2(),
                m.a3(), m.b3(), m.c3(), m.d3(),
                m.a4(), m.b4(), m.c4(), m.d4());
    }

    private static void collectJoints(AINode node, int parent, List<Skeleton.Joint> joints,
                                      Map<String, Integer> names) {
        int id = joints.size();
        String name = node.mName().dataString();
        names.merge(name, id, (first, second) -> -1);
        joints.add(new Skeleton.Joint(name, parent, matrix(node.mTransformation())));
        for (int i = 0; i < node.mNumChildren(); i++) {
            collectJoints(AINode.create(node.mChildren().get(i)), id, joints, names);
        }
    }

    private static int collect(AIScene scene, AINode node, Matrix4f parent, Part[] meshes,
                               List<Part> parts, Skeleton skeleton, Map<String, Integer> names, int id) {
        var transform = new Matrix4f(parent).mul(matrix(node.mTransformation()));
        for (int i = 0; i < node.mNumMeshes(); i++) {
            int index = node.mMeshes().get(i);
            var mesh = AIMesh.create(scene.mMeshes().get(index));
            if ((mesh.mPrimitiveTypes() & aiPrimitiveType_TRIANGLE) == 0) {
                continue;
            }
            if (meshes[index] == null) {
                meshes[index] = copyMesh(mesh, skeleton, names);
            }
            var part = meshes[index];
            parts.add(new Part(part.name(), part.mesh(), new Matrix4f(transform),
                    part.materialIndex(), part.boneNames(), part.skin(), id));
        }
        int next = id + 1;
        for (int i = 0; i < node.mNumChildren(); i++) {
            next = collect(scene, AINode.create(node.mChildren().get(i)), transform, meshes,
                    parts, skeleton, names, next);
        }
        return next;
    }

    private static Part copyMesh(AIMesh mesh, Skeleton skeleton, Map<String, Integer> names) {
        var positions = mesh.mVertices();
        var normals = mesh.mNormals();
        var uv = mesh.mTextureCoords(0);
        float[] vertices = new float[mesh.mNumVertices() * 8];
        for (int i = 0; i < mesh.mNumVertices(); i++) {
            int base = i * 8;
            var position = positions.get(i);
            vertices[base] = position.x();
            vertices[base + 1] = position.y();
            vertices[base + 2] = position.z();
            if (uv != null) {
                vertices[base + 3] = uv.get(i).x();
                vertices[base + 4] = uv.get(i).y();
            }
            if (normals != null) {
                var normal = normals.get(i);
                vertices[base + 5] = normal.x();
                vertices[base + 6] = normal.y();
                vertices[base + 7] = normal.z();
            }
        }
        int[] indices = new int[mesh.mNumFaces() * 3];
        int count = 0;
        for (int i = 0; i < mesh.mNumFaces(); i++) {
            var face = mesh.mFaces().get(i);
            if (face.mNumIndices() == 3) {
                for (int j = 0; j < 3; j++) indices[count++] = face.mIndices().get(j);
            }
        }
        var data = new MeshData(vertices, java.util.Arrays.copyOf(indices, count),
                new VertexLayout().add(0, 3).add(1, 2).add(2, 3));
        var boneNames = new ArrayList<String>();
        MeshSkin skin = null;
        if (mesh.mNumBones() > 0) {
            var bones = new ArrayList<MeshSkin.Bone>();
            var weights = new ArrayList<List<MeshSkin.Influence>>(mesh.mNumVertices());
            for (int i = 0; i < mesh.mNumVertices(); i++) weights.add(new ArrayList<>());
            for (int i = 0; i < mesh.mNumBones(); i++) {
                var bone = AIBone.create(mesh.mBones().get(i));
                String name = bone.mName().dataString();
                boneNames.add(name);
                bones.add(new MeshSkin.Bone(joint(names, name), matrix(bone.mOffsetMatrix())));
                for (int j = 0; j < bone.mNumWeights(); j++) {
                    var weight = bone.mWeights().get(j);
                    if (weight.mWeight() > 0) {
                        weights.get(weight.mVertexId()).add(new MeshSkin.Influence(i, weight.mWeight()));
                    }
                }
            }
            skin = new MeshSkin(skeleton, data, bones, weights);
        }
        return new Part(mesh.mName().dataString(), data, new Matrix4f(), mesh.mMaterialIndex(),
                List.copyOf(boneNames), skin, -1);
    }

    private static int joint(Map<String, Integer> names, String name) {
        int id = names.getOrDefault(name, -1);
        if (id < 0) throw new ResourceException("Missing or ambiguous animation node: " + name);
        return id;
    }

    private static AnimationClip copyAnimation(AIAnimation animation, Map<String, Integer> names) {
        double rate = animation.mTicksPerSecond();
        if (!Double.isFinite(rate) || rate <= 0) rate = 25;
        double duration = Math.max(0, animation.mDuration() / rate);
        var tracks = new HashMap<Integer, AnimationClip.Track>();
        for (int i = 0; i < animation.mNumChannels(); i++) {
            var channel = AINodeAnim.create(animation.mChannels().get(i));
            var rotations = new ArrayList<AnimationClip.RotationKey>();
            for (int k = 0; k < channel.mNumRotationKeys(); k++) {
                var key = channel.mRotationKeys().get(k);
                var q = key.mValue();
                rotations.add(new AnimationClip.RotationKey(key.mTime() / rate, q.x(), q.y(), q.z(), q.w()));
            }
            rotations.sort(Comparator.comparingDouble(AnimationClip.RotationKey::time));
            var positions = copyVectors(channel.mPositionKeys(), channel.mNumPositionKeys(), rate);
            var scales = copyVectors(channel.mScalingKeys(), channel.mNumScalingKeys(), rate);
            if (!positions.isEmpty()) duration = Math.max(duration, positions.getLast().time());
            if (!rotations.isEmpty()) duration = Math.max(duration, rotations.getLast().time());
            if (!scales.isEmpty()) duration = Math.max(duration, scales.getLast().time());
            tracks.put(joint(names, channel.mNodeName().dataString()),
                    new AnimationClip.Track(positions, rotations, scales));
        }
        return new AnimationClip(animation.mName().dataString(), duration, tracks);
    }

    private static List<AnimationClip.VectorKey> copyVectors(AIVectorKey.Buffer source, int count, double rate) {
        var keys = new ArrayList<AnimationClip.VectorKey>(count);
        for (int i = 0; i < count; i++) {
            var key = source.get(i);
            var v = key.mValue();
            keys.add(new AnimationClip.VectorKey(key.mTime() / rate, v.x(), v.y(), v.z()));
        }
        keys.sort(Comparator.comparingDouble(AnimationClip.VectorKey::time));
        return keys;
    }
}

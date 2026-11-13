package com.melon.foolsEngine.util;

import com.melon.foolsEngine.api.rendering.resource.MeshData;
import com.melon.foolsEngine.exception.ResourceException;
import org.joml.Matrix4f;
import org.lwjgl.assimp.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.lwjgl.assimp.Assimp.*;

/** Imports CPU mesh data. Returned data does not depend on an open Assimp scene. */
public final class MeshImporter {
    private MeshImporter() {}

    /** Bone names and animation metadata reserved for later skinning and playback. */
    public record Animation(String name, double durationTicks, double ticksPerSecond, int channels) {}

    public record Part(String name, MeshData mesh, Matrix4f transform, int materialIndex,
                       List<String> boneNames) {}

    public record Model(List<Part> parts, List<Animation> animations) {}

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
            if (scene.mRootNode() != null) {
                collect(scene, scene.mRootNode(), new Matrix4f(), new Part[scene.mNumMeshes()], parts);
            }
            if (parts.isEmpty()) {
                throw new ResourceException("No triangle meshes in " + path);
            }
            var animations = new ArrayList<Animation>();
            for (int i = 0; i < scene.mNumAnimations(); i++) {
                var animation = AIAnimation.create(scene.mAnimations().get(i));
                animations.add(new Animation(animation.mName().dataString(), animation.mDuration(),
                        animation.mTicksPerSecond(), animation.mNumChannels()));
            }
            return new Model(List.copyOf(parts), List.copyOf(animations));
        } finally {
            aiReleaseImport(scene);
        }
    }

    private static void collect(AIScene scene, AINode node, Matrix4f parent, Part[] meshes, List<Part> parts) {
        var m = node.mTransformation();
        var transform = new Matrix4f(parent).mul(new Matrix4f(
                m.a1(), m.b1(), m.c1(), m.d1(),
                m.a2(), m.b2(), m.c2(), m.d2(),
                m.a3(), m.b3(), m.c3(), m.d3(),
                m.a4(), m.b4(), m.c4(), m.d4()));
        for (int i = 0; i < node.mNumMeshes(); i++) {
            int index = node.mMeshes().get(i);
            var mesh = AIMesh.create(scene.mMeshes().get(index));
            if ((mesh.mPrimitiveTypes() & aiPrimitiveType_TRIANGLE) == 0) {
                continue;
            }
            if (meshes[index] == null) {
                meshes[index] = copyMesh(mesh);
            }
            var part = meshes[index];
            parts.add(new Part(part.name(), part.mesh(), new Matrix4f(transform),
                    part.materialIndex(), part.boneNames()));
        }
        for (int i = 0; i < node.mNumChildren(); i++) {
            collect(scene, AINode.create(node.mChildren().get(i)), transform, meshes, parts);
        }
    }

    private static Part copyMesh(AIMesh mesh) {
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
        var boneNames = new ArrayList<String>();
        for (int i = 0; i < mesh.mNumBones(); i++) {
            boneNames.add(AIBone.create(mesh.mBones().get(i)).mName().dataString());
        }
        var data = new MeshData(vertices, java.util.Arrays.copyOf(indices, count),
                new VertexLayout().add(0, 3).add(1, 2).add(2, 3));
        return new Part(mesh.mName().dataString(), data, new Matrix4f(), mesh.mMaterialIndex(), List.copyOf(boneNames));
    }
}

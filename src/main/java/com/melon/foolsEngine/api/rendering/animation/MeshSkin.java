package com.melon.foolsEngine.api.rendering.animation;

import com.melon.foolsEngine.api.rendering.resource.MeshData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

import java.util.List;

/** Mesh-local skinning data. Offsets transform bind vertices into each joint's space. */
public final class MeshSkin {
    public record Bone(int joint, Matrix4fc offset) {
        public Bone {
            offset = new Matrix4f(offset);
        }

        @Override
        public Matrix4fc offset() { return new Matrix4f(offset); }
    }

    public record Influence(int bone, float weight) {}

    private final Skeleton skeleton;
    private final MeshData mesh;
    private final List<Bone> bones;
    private final List<List<Influence>> weights;
    private final int stride;
    private final int positionOffset;
    private final int normalOffset;

    public MeshSkin(Skeleton skeleton, MeshData mesh, List<Bone> bones, List<List<Influence>> weights) {
        this.skeleton = skeleton;
        this.mesh = new MeshData(mesh.vertices().clone(), mesh.indices(), mesh.layout());
        this.bones = List.copyOf(bones);
        this.weights = weights.stream().map(List::copyOf).toList();
        stride = mesh.layout().stride();
        positionOffset = offset(mesh, 0);
        normalOffset = offset(mesh, 2);
        if (positionOffset < 0 || mesh.vertices().length % stride != 0
                || this.weights.size() != mesh.vertices().length / stride) {
            throw new IllegalArgumentException("Skin weights must match the mesh vertices");
        }
        for (Bone bone : bones) {
            if (bone.joint < 0 || bone.joint >= skeleton.joints().size()) {
                throw new IllegalArgumentException("Bone references an unknown joint");
            }
        }
        for (var influences : this.weights) {
            for (var influence : influences) {
                if (influence.bone < 0 || influence.bone >= bones.size()
                        || !Float.isFinite(influence.weight) || influence.weight <= 0) {
                    throw new IllegalArgumentException("Invalid vertex influence");
                }
            }
        }
    }

    private static int offset(MeshData mesh, int location) {
        return mesh.layout().attributes().stream()
                .filter(attribute -> attribute.location() == location && attribute.size() >= 3)
                .mapToInt(attribute -> attribute.offset()).findFirst().orElse(-1);
    }

    /** Returns deformed interleaved vertices. Apply meshTransform once when drawing them. */
    public float[] deform(AnimationPlayer player, Matrix4fc meshTransform) {
        if (player.skeleton() != skeleton) throw new IllegalArgumentException("Skin and player need the same skeleton");
        float determinant = meshTransform.determinant3x3();
        if (!Float.isFinite(determinant) || determinant == 0) {
            throw new IllegalArgumentException("Skinning requires an invertible mesh transform");
        }
        var inverseMesh = new Matrix4f(meshTransform).invert();
        var palette = new Matrix4f[bones.size()];
        var normalPalette = normalOffset < 0 ? null : new Matrix3f[bones.size()];
        for (int i = 0; i < bones.size(); i++) {
            var bone = bones.get(i);
            palette[i] = new Matrix4f(inverseMesh).mul(player.jointTransform(bone.joint)).mul(bone.offset);
            if (normalPalette != null) {
                var normalMatrix = new Matrix3f(palette[i]);
                if (normalMatrix.determinant() != 0) normalMatrix.invert().transpose();
                normalPalette[i] = normalMatrix;
            }
        }

        float[] source = mesh.vertices();
        float[] vertices = source.clone();
        var position = new Vector3f();
        var normal = new Vector3f();
        var transformed = new Vector3f();
        for (int i = 0; i < weights.size(); i++) {
            var influences = weights.get(i);
            if (influences.isEmpty()) continue;
            int p = i * stride + positionOffset;
            int n = i * stride + normalOffset;
            position.zero();
            normal.zero();
            double sum = 0;
            for (var influence : influences) {
                int bone = influence.bone;
                float weight = influence.weight;
                sum += weight;
                transformed.set(source[p], source[p + 1], source[p + 2]);
                palette[bone].transformPosition(transformed);
                position.fma(weight, transformed);
                if (normalPalette != null) {
                    transformed.set(source[n], source[n + 1], source[n + 2]);
                    normalPalette[bone].transform(transformed);
                    normal.fma(weight, transformed);
                }
            }
            position.mul((float) (1 / sum));
            vertices[p] = position.x;
            vertices[p + 1] = position.y;
            vertices[p + 2] = position.z;
            if (normalPalette != null && normal.lengthSquared() > 0) {
                normal.normalize();
                vertices[n] = normal.x;
                vertices[n + 1] = normal.y;
                vertices[n + 2] = normal.z;
            }
        }
        return vertices;
    }
}

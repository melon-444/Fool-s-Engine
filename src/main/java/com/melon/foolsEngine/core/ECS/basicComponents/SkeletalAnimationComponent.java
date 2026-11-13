package com.melon.foolsEngine.core.ECS.basicComponents;

import com.melon.foolsEngine.api.rendering.animation.AnimationPlayer;
import com.melon.foolsEngine.api.rendering.animation.MeshSkin;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.Objects;

/** Associates one renderable mesh part with a model pose. Parts may share one player. */
public final class SkeletalAnimationComponent extends Component {
    public final AnimationPlayer player;
    public final MeshSkin skin;
    public final int meshNode;
    private final Matrix4fc skinSpace;
    private long cachedVersion = -1;
    private float[] cachedVertices;

    public SkeletalAnimationComponent(AnimationPlayer player, MeshSkin skin, int meshNode) {
        this.player = Objects.requireNonNull(player);
        this.skin = skin;
        if (meshNode < 0 || meshNode >= player.skeleton().joints().size()) {
            throw new IllegalArgumentException("Unknown mesh node: " + meshNode);
        }
        this.meshNode = meshNode;
        this.skinSpace = skin == null ? null : player.bindTransform(meshNode, new Matrix4f());
    }

    /** Resolves the basis used to draw the part, relative to the model root. */
    public Matrix4f meshTransform(Matrix4f out) {
        player.jointTransform(meshNode, out);
        // The animated mesh basis cannot be inverted at zero scale.
        return skinSpace != null && out.determinant3x3() == 0 ? out.set(skinSpace) : out;
    }

    /** The returned array is borrowed and must not be modified. */
    public float[] deformedVertices() {
        if (skin == null) return null;
        long version = player.poseVersion();
        if (cachedVersion != version) {
            cachedVertices = skin.deform(player, meshTransform(new Matrix4f()));
            cachedVersion = version;
        }
        return cachedVertices;
    }
}

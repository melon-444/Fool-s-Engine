package com.melon.foolsEngineTest;

import com.melon.foolsEngine.api.APIBuilder;
import com.melon.foolsEngine.api.rendering.animation.AnimationPlayer;
import com.melon.foolsEngine.api.rendering.resource.*;
import com.melon.foolsEngine.api.rendering.shader.BuiltinShaders;
import com.melon.foolsEngine.api.rendering.shader.ShaderPass;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.core.ECS.basicComponents.*;
import com.melon.foolsEngine.core.bootstrap.EngineBoot;
import com.melon.foolsEngine.util.MeshImporter;
import com.melon.foolsEngine.util.logger.Logger;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

/** Visual smoke test using an external asset. Run with FOOLSENGINE_ANIMATION_ASSET set. */
public final class TestSkeletalAnimation {
    public static void main(String[] args) throws Exception {
        String asset = args.length == 0 ? System.getenv("FOOLSENGINE_ANIMATION_ASSET") : args[0];
        if (asset == null || asset.isBlank()) {
            throw new IllegalArgumentException("Set FOOLSENGINE_ANIMATION_ASSET to an animated model");
        }
        var model = MeshImporter.load(Path.of(asset));
        var clip = model.clips().stream().filter(c -> c.durationSeconds() > 0).findFirst().orElseThrow();
        var player = new AnimationPlayer(model.skeleton());
        var api = APIBuilder.builder().OpenGL().create();
        var engine = EngineBoot.create(api, Math.max(64, model.parts().size() + 2), 32, 960, 720, false);
        var window = engine.mainWindow;
        var meshes = new ArrayList<Mesh>();
        var lighting = new LightEnvironment();
        BuiltinShaders shaders = null;
        Texture texture = null;
        try {
            shaders = BuiltinShaders.load(api);
            var material = new Material(shaders.main());
            texture = api.getTexture();
            Path imageFile = Files.createTempFile("foolsengine-animation-", ".png");
            try {
                var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                image.setRGB(0, 0, 0xfff0d8ae);
                ImageIO.write(image, "png", imageFile.toFile());
                texture.upload(imageFile);
            } finally {
                Files.deleteIfExists(imageFile);
            }
            material.set("textureSampler", texture);
            var entities = engine.entityManager;
            var min = new Vector3f(Float.POSITIVE_INFINITY);
            var max = new Vector3f(Float.NEGATIVE_INFINITY);
            var position = new Vector3f();
            for (var part : model.parts()) {
                var mesh = api.getMesh();
                mesh.upload(part.mesh());
                meshes.add(mesh);
                int entity = entities.createEntity();
                entities.bindComponent(entity, new TransformComponent());
                entities.bindComponent(entity, new RenderableComponent(mesh, material));
                entities.bindComponent(entity, new SkeletalAnimationComponent(player, part.skin(), part.node()));
                float[] vertices = part.mesh().vertices();
                for (int i = 0; i < vertices.length; i += part.mesh().layout().stride()) {
                    part.transform().transformPosition(position.set(vertices[i], vertices[i + 1], vertices[i + 2]));
                    min.min(position);
                    max.max(position);
                }
            }
            float radius = Math.max(max.distance(min) * 0.5f, 0.01f);
            var center = new Vector3f(min).lerp(max, 0.5f);
            int camera = entities.createEntity();
            entities.bindComponent(camera, new TransformComponent(center.add(0, 0, 3.5f * radius)));
            entities.bindComponent(camera, new CameraComponent(40, radius * 0.01f));
            entities.bindComponent(entities.createEntity(), new RenderPassComponent(0, ShaderPass.core().build()));
            lighting.setAmbient(0.45f, 0.45f, 0.45f);
            lighting.add(Light.directional(new Vector3f(1), new Vector3f(0.5f, -1, -1)));
            engine.systemScheduler.getScene().setLighting(lighting);
            engine.systemScheduler.getScene().setBackGroundColor(0.15f, 0.18f, 0.22f, 1);
            player.play(clip, true);
            window.setTitle("Skeletal animation — " + clip.name());
            window.show();
            int frameLimit = Integer.parseInt(System.getenv().getOrDefault("FOOLSENGINE_ANIMATION_FRAMES", "0"));
            for (int frame = 0; !window.shouldClose() && (frameLimit == 0 || frame < frameLimit); frame++) {
                engine.systemScheduler.update();
                if (engine.frame.getDrawCallCount() == 0) throw new IllegalStateException("No animation draw calls");
            }
        } finally {
            engine.systemScheduler.shutdown();
            for (var mesh : meshes) mesh.destroy();
            if (texture != null) texture.close();
            if (shaders != null) shaders.close();
            lighting.destroy();
            api.getWindowsManager().destroyWindow(window, true);
        }
    }
}

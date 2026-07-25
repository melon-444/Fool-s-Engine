package com.melon.foolsEngineTest;

import com.melon.foolsEngine.api.rendering.render.RenderCommand;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.api.rendering.resource.*;
import com.melon.foolsEngine.api.rendering.resource.texture.Texture;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.Window;
import com.melon.foolsEngine.api.windows.WindowsManager;
import com.melon.foolsEngine.core.ECS.basicComponents.TransformComp;
import com.melon.foolsEngine.core.EngineBoot;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.util.ObjLoader;
import com.melon.foolsEngine.util.OrthogonalProjection;
import com.melon.foolsEngine.util.PerspectiveProjection;
import com.melon.foolsEngine.util.VertexLayout;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.file.Path;

@Deprecated
public class TestBackend {
    static FoolsEngine foolsEngine = EngineBoot.create(1000, 100, 800, 600, false);

    public static void main(String[] args) {
        WindowsManager manager = foolsEngine.serviceFactory.getWindowsManager();

        Window win = foolsEngine.mainWindow;
        win.setTitle("测试");


        Mesh dragon_mesh = foolsEngine.serviceFactory.getMesh();
        dragon_mesh.upload(ObjLoader.loadMesh(Path.of("src/test/resources/shaders/model/dragon.obj")));

        float[] vertices = new float[]{.5f, -.5f, 0.f,
                0f, 0f,//0
                .5f, .5f, 0.f,
                0f, 1f,//1
                -.5f, .5f, 0.f,
                1f, 1f,//2
                -.5f, -.5f, 0.f,
                1f, 0f,//3
        };
        int[] indices = new int[]{0, 3, 1, 1, 3, 2};

        MeshData squareData = new MeshData(vertices, indices, new VertexLayout().add(0, 3).add(1, 2));
        Mesh square_mesh = foolsEngine.serviceFactory.getMesh();
        square_mesh.upload(squareData);

        RenderFrame frame = foolsEngine.frame;

        ShaderProgram shader = foolsEngine.serviceFactory.getShaderProgram();
        shader.load(Path.of("src/main/resources/shader/main/main_vsh.glsl"), Path.of("src/main/resources/shader/main/main_fsh.glsl"));
        Material material = new Material(shader);
        Texture texture = foolsEngine.serviceFactory.getTexture();
        texture.upload(Path.of("src/test/resources/textures/test2.png"));
        Texture texture1 = foolsEngine.serviceFactory.getTexture();
        texture1.upload(Path.of("src/test/resources/textures/test1.png"));
        material.set("textureSampler", texture);

        Vector3f position = new Vector3f(0.0f, -0.5f, 0);
        TransformComp trans = new TransformComp(position, new Quaternionf(), new Vector3f(0.1f, 0.1f, 0.1f));

        float distance = 200f;
        float size = distance * Math.tan(Math.toRadians(foolsEngine.FOV / 2)) / 100;

        PerspectiveProjection proj = new PerspectiveProjection(foolsEngine.FOV, foolsEngine.aspect, foolsEngine.Z_NEAR);
        OrthogonalProjection o_proj = new OrthogonalProjection(size * foolsEngine.aspect, size, foolsEngine.Z_NEAR, distance);
        Camera camera = new Camera(new Matrix4f().lookAt(0, 0, -1, 0, 0, 0, 0, 1, 0), proj.get(new Matrix4f()));


        win.show();
        frame.init();

        int deg = 0;

        win.setFullscreen(false);

        LightEnvironment lightEnv = new LightEnvironment();
        lightEnv.setAmbient(0.08f, 0.08f, 0.08f);
        lightEnv.setShadowMapSize(4096);
        lightEnv.add(Light.spot(new Vector3f(1.0f), new Vector3f(0, -1f, 0), new Vector3f(0, 5, 0), 10f, 10f));
        RenderScene scene = new RenderScene();
        scene.setBackGroundColor(0, 0.75f, 0, 1);
        scene.setLighting(lightEnv);


        while (!win.shouldClose()) {
            scene.setCamera(camera);
            scene.submit(new RenderCommand(dragon_mesh, material, trans.getMatrix()));
            frame.render(scene);
            win.update();

            camera.view.identity().lookAt(-3 * Math.sin(Math.toRadians(deg)), 0, -3 * Math.cos(Math.toRadians(deg)), 0, 0, 0, 0, 1, 0);
            ++deg;
            deg %= 360;
            if (deg < 180) material.set("textureSampler", texture);
            else material.set("textureSampler", texture1);
        }
        shader.destroy();
        manager.destroyWindow(win, true);
    }

}

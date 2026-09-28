package com.melon.snake3d;

import com.melon.foolsEngine.api.input.Action;
import com.melon.foolsEngine.api.input.FoolsEngineKeyCode;
import com.melon.foolsEngine.api.input.InputManager;
import com.melon.foolsEngine.api.rendering.render.RenderCommand;
import com.melon.foolsEngine.api.rendering.render.RenderFrame;
import com.melon.foolsEngine.api.rendering.render.RenderScene;
import com.melon.foolsEngine.api.rendering.resource.Camera;
import com.melon.foolsEngine.api.rendering.resource.Material;
import com.melon.foolsEngine.api.rendering.resource.Mesh;
import com.melon.foolsEngine.api.rendering.shader.ShaderPass;
import com.melon.foolsEngine.api.rendering.shader.ShaderProgram;
import com.melon.foolsEngine.api.windows.Window;
import com.melon.foolsEngine.api.windows.WindowsManager;
import com.melon.foolsEngine.core.FoolsEngine;
import com.melon.foolsEngine.core.bootstrap.EngineBoot;
import com.melon.foolsEngine.util.CursorMode;
import com.melon.foolsEngine.util.PerspectiveProjection;
import com.melon.foolsEngine.util.RenderQueue;
import com.melon.foolsEngine.util.SignalType;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Snake3D — a tiny closed-loop demo game on foolsEngine, played inside a 3D
 * cube. Everything is a procedurally-generated unit cube/quad, shaded with flat
 * colors; no textures or external models are required.
 *
 * <p>The snake glides smoothly between cells. Crossing a face of the cube wraps
 * to the opposite face through a glowing portal, which erupts with a
 * shader-driven swirl oriented perpendicular to travel; the swirl disappears
 * once the snake leaves. The orbit camera follows the head (smoothed) and the
 * scroll wheel zooms.</p>
 */
public class SnakeGame {

    private static final int SIZE = 12;
    private static final float SEG_SIZE = 0.9f;
    private static final float FOOD_SIZE = 0.7f;
    private static final float SWIRL_RADIUS = 1.8f;
    private static final float LOOK_SENSITIVITY = 0.25f;

    private static final float GRID_THICKNESS = 0.05f;
    private static final float GLOW_RADIUS = SIZE / 5f;
    private static final float CAM_SMOOTH = 0.18f;
    private static final float MIN_RADIUS = 12f;
    private static final float MAX_RADIUS = 60f;

    /** A live portal swirl. Removed once no snake segment is near it. */
    private static final class Swirl {
        final Vector3f pos;
        final Snake.Dir dir;
        final Material material;

        Swirl(Vector3f pos, Snake.Dir dir, Material material) {
            this.pos = pos;
            this.dir = dir;
            this.material = material;
        }
    }

    public static void main(String[] args) {
        FoolsEngine engine = EngineBoot.create(20000, 100, 1280, 720, false);
        WindowsManager winMgr = engine.serviceFactory.getWindowsManager();
        Window win = engine.mainWindow;
        win.setTitle("Snake3D");
        win.setSize(1280, 720);
        win.show();

        RenderFrame frame = engine.frame;

        ShaderProgram flat = engine.serviceFactory.getShaderProgram();
        flat.load(
                Path.of("games/snake3d/src/main/resources/shaders/flat_vsh.glsl"),
                Path.of("games/snake3d/src/main/resources/shaders/flat_fsh.glsl"));

        ShaderProgram portal = engine.serviceFactory.getShaderProgram();
        portal.load(
                Path.of("games/snake3d/src/main/resources/shaders/portal_vsh.glsl"),
                Path.of("games/snake3d/src/main/resources/shaders/portal_fsh.glsl"));

        ShaderProgram gridShader = engine.serviceFactory.getShaderProgram();
        gridShader.load(
                Path.of("games/snake3d/src/main/resources/shaders/portal_vsh.glsl"),
                Path.of("games/snake3d/src/main/resources/shaders/grid_fsh.glsl"));

        ShaderProgram swirlShader = engine.serviceFactory.getShaderProgram();
        swirlShader.load(
                Path.of("games/snake3d/src/main/resources/shaders/swirl_vsh.glsl"),
                Path.of("games/snake3d/src/main/resources/shaders/swirl_fsh.glsl"));

        Material matBody = material(flat, 0.15f, 0.70f, 0.25f);
        Material matHead = material(flat, 0.35f, 0.95f, 0.45f);
        Material matFood = material(flat, 0.90f, 0.20f, 0.20f);

        Material matPortal = new Material(portal);
        matPortal.set("uColor", new Vector3f(0.20f, 0.85f, 1.0f));
        matPortal.setQueue(RenderQueue.TRANSPARENT);

        Material matGrid = new Material(gridShader);
        matGrid.set("uColor", new Vector3f(0.35f, 0.75f, 1.0f));
        matGrid.set("uRadius", GLOW_RADIUS);
        matGrid.set("uHeadPos", new Vector3f());
        matGrid.setQueue(RenderQueue.TRANSPARENT);

        Mesh unitCube = engine.serviceFactory.getMesh();
        unitCube.upload(MeshFactory.box(1f, 1f, 1f));
        Mesh quadMesh = engine.serviceFactory.getMesh();
        quadMesh.upload(MeshFactory.quad());

        List<Matrix4f> frameTransforms = buildFrameTransforms();
        List<Matrix4f> gridTransforms = buildGridTransforms();

        Camera camera = buildCamera(win);
        float yaw = 35f;
        float pitch = 25f;
        float orbitRadius = 30f;

        InputManager input = engine.serviceFactory.createInputManager(win);
        win.setCursorMode(CursorMode.DISABLED);

        Action north = () -> SignalType.BUTTON;
        Action south = () -> SignalType.BUTTON;
        Action west = () -> SignalType.BUTTON;
        Action east = () -> SignalType.BUTTON;
        Action goUp = () -> SignalType.BUTTON;
        Action goDown = () -> SignalType.BUTTON;
        Action restart = () -> SignalType.BUTTON;
        Action quit = () -> SignalType.BUTTON;
        Action moreFood = () -> SignalType.BUTTON;
        Action lessFood = () -> SignalType.BUTTON;
        Action faster = () -> SignalType.BUTTON;
        Action slower = () -> SignalType.BUTTON;
        Action look = () -> SignalType.AXIS_2DDel;
        Action zoom = () -> SignalType.AXIS_1DDel;
        Action toggleCursor = () -> SignalType.BUTTON;

        input.bind(input.getKeyboard(), FoolsEngineKeyCode.W, north);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.S, south);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.A, west);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.D, east);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.Q, goUp);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.E, goDown);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.R, restart);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.ESC, quit);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.F, moreFood);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.G, lessFood);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.P, faster);
        input.bind(input.getKeyboard(), FoolsEngineKeyCode.O, slower);
        input.bind(input.getMouse(), FoolsEngineKeyCode.CURSOR, look);
        input.bind(input.getMouse(), FoolsEngineKeyCode.MOUSE_MIDDLE, zoom);
        input.bind(input.getMouse(), FoolsEngineKeyCode.MOUSE_RIGHT, toggleCursor);

        RenderScene scene = new RenderScene();
        Snake snake = new Snake(SIZE, SIZE, SIZE);
        int highScore = 0;
        List<Swirl> swirls = new ArrayList<>();
        Vector3f camTarget = headWorld(snake, 0f);

        float baseInterval = 0.18f;
        float acc = 0f;
        long last = System.nanoTime();
        float time = 0f;

        while (!win.shouldClose()) {
            long now = System.nanoTime();
            float dt = Math.min((now - last) / 1e9f, 0.05f);
            last = now;
            time += dt;

            input.updateFromPolledInputs();

            if (input.isActionPressed(quit)) {
                break;
            }
            if (input.isActionPressed(restart)) {
                snake = new Snake(SIZE, SIZE, SIZE);
                swirls.clear();
                camTarget.set(headWorld(snake, 0f));
            }

            if (input.isActionPressed(north)) {
                snake.setDirection(Snake.Dir.NORTH);
            }
            if (input.isActionPressed(south)) {
                snake.setDirection(Snake.Dir.SOUTH);
            }
            if (input.isActionPressed(west)) {
                snake.setDirection(Snake.Dir.WEST);
            }
            if (input.isActionPressed(east)) {
                snake.setDirection(Snake.Dir.EAST);
            }
            if (input.isActionPressed(goUp)) {
                snake.setDirection(Snake.Dir.UP);
            }
            if (input.isActionPressed(goDown)) {
                snake.setDirection(Snake.Dir.DOWN);
            }

            if (input.isActionPressed(moreFood)) {
                snake.setFoodCount(snake.foodCount() + 1);
            }
            if (input.isActionPressed(lessFood)) {
                snake.setFoodCount(snake.foodCount() - 1);
            }
            if (input.isActionPressed(faster)) {
                baseInterval = Math.max(0.03f, baseInterval - 0.02f);
            }
            if (input.isActionPressed(slower)) {
                baseInterval = Math.min(0.60f, baseInterval + 0.02f);
            }

            if (input.isActionPressed(toggleCursor)) {
                if (win.getCursorMode() == CursorMode.DISABLED) {
                    win.setCursorMode(CursorMode.NORMAL);
                } else {
                    win.setCursorMode(CursorMode.DISABLED);
                }
                input.getMouse().flushDeltas();
            }

            float scroll = input.getActionAxis1DDelta(zoom);
            if (scroll != 0f) {
                orbitRadius = Math.min(MAX_RADIUS, Math.max(MIN_RADIUS,
                        orbitRadius * (float) Math.exp(-scroll * 0.15f)));
            }

            Vector2f md = win.getCursorMode() == CursorMode.DISABLED
                    ? input.getActionAxis2DDelta(look)
                    : new Vector2f(0f, 0f);
            yaw -= md.x * LOOK_SENSITIVITY;
            pitch = Math.min(89f, Math.max(-89f, pitch - md.y * LOOK_SENSITIVITY));

            float tickInterval = Math.max(0.03f, baseInterval - snake.score() * 0.004f);
            if (snake.isAlive()) {
                acc += dt;
                while (acc >= tickInterval && snake.isAlive()) {
                    snake.step();
                    if (snake.isAlive() && snake.headWrapped()) {
                        spawnSwirls(swirls, snake.head(), snake.wrapDir(), swirlShader);
                    }
                    acc -= tickInterval;
                }
            }

            float snakeT = Math.min(Math.max(acc / tickInterval, 0f), 1f);
            Vector3f head = headWorld(snake, snakeT);
            camTarget.lerp(head, 1f - (float) Math.exp(-dt / CAM_SMOOTH));
            updateCamera(camera, camTarget, yaw, pitch, orbitRadius);

            matGrid.set("uHeadPos", head);

            scene.clear();
            scene.setCamera(camera);
            if (snake.isAlive()) {
                scene.setBackGroundColor(0.04f, 0.04f, 0.07f, 1f);
            } else {
                scene.setBackGroundColor(0.16f, 0.03f, 0.03f, 1f);
            }

            submitTransforms(scene, unitCube, matGrid, gridTransforms);
            submitTransforms(scene, unitCube, matPortal, frameTransforms);
            submitSnake(scene, unitCube, snake, matHead, matBody, snakeT);

            for (Snake.Cell f : snake.foods()) {
                float phase = (f.x() + f.y() + f.z()) * 0.7f;
                float pulse = FOOD_SIZE * (1f + 0.15f * (float) Math.sin(time * 6f + phase));
                scene.submit(new RenderCommand(unitCube, matFood,
                        new Matrix4f().translation(
                                f.x() + 0.5f - SIZE / 2f,
                                f.y() + 0.5f - SIZE / 2f,
                                f.z() + 0.5f - SIZE / 2f)
                                .scale(pulse, pulse, pulse)));
            }

            for (Iterator<Swirl> it = swirls.iterator(); it.hasNext(); ) {
                Swirl s = it.next();
                float near = minDistToSnake(snake, s.pos);
                if (near > SWIRL_RADIUS) {
                    it.remove();
                    continue;
                }
                float k = 1f - near / SWIRL_RADIUS;
                s.material.set("uIntensity", k * k);
                float scale = 2.2f + 1.3f * k;
                scene.submit(new RenderCommand(quadMesh, s.material,
                        swirlTransform(s.pos, s.dir, scale, false)));
                scene.submit(new RenderCommand(quadMesh, s.material,
                        swirlTransform(s.pos, s.dir, scale, true)));
            }

            scene.submitPass(ShaderPass.core().build());
            scene.submitPass(ShaderPass.core()
                    .queue(RenderQueue.TRANSPARENT)
                    .additive()
                    .uniform("uTime", time)
                    .build());
            frame.render(scene);

            if (!snake.isAlive()) {
                highScore = Math.max(highScore, snake.score());
            }

            String status = snake.isAlive()
                    ? "Score " + snake.score() + "   Length " + snake.length()
                    : (snake.won() ? "YOU WIN! " : "GAME OVER  ")
                            + "Score " + snake.score();
            int tickMs = Math.round(tickInterval * 1000f);
            win.setTitle("Snake3D  |  " + status + "  |  High " + highScore
                    + "  |  Food " + snake.foodCount() + "  Tick " + tickMs + "ms"
                    + "  |  WASD move, Q/E up-down, mouse orbit, wheel zoom,"
                    + " RMB cursor, R restart, F/G food, P/O speed, ESC quit");

            input.clearPolledInputs();
            win.update();

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        flat.destroy();
        portal.destroy();
        gridShader.destroy();
        swirlShader.destroy();
        unitCube.destroy();
        quadMesh.destroy();
        winMgr.destroyWindow(win, true);
    }

    private static void submitTransforms(RenderScene scene, Mesh cube, Material mat,
                                         List<Matrix4f> transforms) {
        for (Matrix4f m : transforms) {
            scene.submit(new RenderCommand(cube, mat, m));
        }
    }

    private static void submitSnake(RenderScene scene, Mesh cube, Snake snake,
                                    Material headMat, Material bodyMat, float t) {
        List<Snake.Cell> cur = snake.body();
        List<Snake.Cell> prev = snake.prevBody();
        if (cur.isEmpty()) {
            return;
        }
        float tt = t;

        Snake.Cell hc = cur.get(0);
        Snake.Cell hp = prev.isEmpty() ? hc : prev.get(0);
        if (snake.headWrapped() && snake.wrapDir() != null) {
            Snake.Dir wd = snake.wrapDir();
            submitSegment(scene, cube, headMat,
                    lerp(hp.x() + 0.5f, hc.x() + 0.5f, tt),
                    lerp(hp.y() + 0.5f, hc.y() + 0.5f, tt),
                    lerp(hp.z() + 0.5f, hc.z() + 0.5f, tt));
            submitSegment(scene, cube, headMat,
                    wrap(hc.x() + 0.5f, SIZE) - wd.dx * (1f - tt),
                    wrap(hc.y() + 0.5f, SIZE) - wd.dy * (1f - tt),
                    wrap(hc.z() + 0.5f, SIZE) - wd.dz * (1f - tt));
        } else {
            submitSegment(scene, cube, headMat,
                    wrap(lerp(hp.x() + 0.5f, hc.x() + 0.5f, tt), SIZE),
                    wrap(lerp(hp.y() + 0.5f, hc.y() + 0.5f, tt), SIZE),
                    wrap(lerp(hp.z() + 0.5f, hc.z() + 0.5f, tt), SIZE));
        }

        for (int i = 1; i < cur.size(); i++) {
            Snake.Cell sc = cur.get(i);
            Snake.Cell sp = prev.size() > i ? prev.get(i) : sc;
            submitSegment(scene, cube, bodyMat,
                    wrap(lerp(sp.x() + 0.5f, sc.x() + 0.5f, tt), SIZE),
                    wrap(lerp(sp.y() + 0.5f, sc.y() + 0.5f, tt), SIZE),
                    wrap(lerp(sp.z() + 0.5f, sc.z() + 0.5f, tt), SIZE));
        }
    }

    private static void submitSegment(RenderScene scene, Mesh cube, Material mat,
                                      float gx, float gy, float gz) {
        scene.submit(new RenderCommand(cube, mat,
                new Matrix4f().translation(
                        gx - SIZE / 2f, gy - SIZE / 2f, gz - SIZE / 2f)
                        .scale(SEG_SIZE, SEG_SIZE, SEG_SIZE)));
    }

    private static List<Matrix4f> buildFrameTransforms() {
        float h = SIZE / 2f;
        float t = 0.06f;
        List<Matrix4f> out = new ArrayList<>();
        for (float y : new float[]{-h, h}) {
            for (float z : new float[]{-h, h}) {
                out.add(new Matrix4f().translation(0f, y, z).scale(SIZE, t, t));
            }
        }
        for (float x : new float[]{-h, h}) {
            for (float z : new float[]{-h, h}) {
                out.add(new Matrix4f().translation(x, 0f, z).scale(t, SIZE, t));
            }
        }
        for (float x : new float[]{-h, h}) {
            for (float y : new float[]{-h, h}) {
                out.add(new Matrix4f().translation(x, y, 0f).scale(t, t, SIZE));
            }
        }
        return out;
    }

    private static List<Matrix4f> buildGridTransforms() {
        float h = SIZE / 2f;
        float t = GRID_THICKNESS;
        List<Matrix4f> out = new ArrayList<>();
        for (int i = 0; i <= SIZE; i++) {
            for (int j = 0; j <= SIZE; j++) {
                float a = -h + i;
                float b = -h + j;
                out.add(new Matrix4f().translation(0f, a, b).scale(SIZE, t, t));
                out.add(new Matrix4f().translation(a, 0f, b).scale(t, SIZE, t));
                out.add(new Matrix4f().translation(a, b, 0f).scale(t, t, SIZE));
            }
        }
        return out;
    }

    private static void spawnSwirls(List<Swirl> swirls, Snake.Cell head,
                                    Snake.Dir wd, ShaderProgram swirlShader) {
        float hx = wrap(head.x() + 0.5f, SIZE) - SIZE / 2f;
        float hy = wrap(head.y() + 0.5f, SIZE) - SIZE / 2f;
        float hz = wrap(head.z() + 0.5f, SIZE) - SIZE / 2f;
        float h = SIZE / 2f;

        Vector3f exit;
        Vector3f entry;
        switch (wd) {
            case EAST -> {
                exit = new Vector3f(h, hy, hz);
                entry = new Vector3f(-h, hy, hz);
            }
            case WEST -> {
                exit = new Vector3f(-h, hy, hz);
                entry = new Vector3f(h, hy, hz);
            }
            case SOUTH -> {
                exit = new Vector3f(hx, hy, h);
                entry = new Vector3f(hx, hy, -h);
            }
            case NORTH -> {
                exit = new Vector3f(hx, hy, -h);
                entry = new Vector3f(hx, hy, h);
            }
            case UP -> {
                exit = new Vector3f(hx, h, hz);
                entry = new Vector3f(hx, -h, hz);
            }
            default -> {
                exit = new Vector3f(hx, -h, hz);
                entry = new Vector3f(hx, h, hz);
            }
        }

        swirls.add(new Swirl(exit, wd, swirlMaterial(swirlShader)));
        swirls.add(new Swirl(entry, wd, swirlMaterial(swirlShader)));
    }

    private static Material swirlMaterial(ShaderProgram swirlShader) {
        Material mat = new Material(swirlShader);
        mat.set("uColor", new Vector3f(0.70f, 0.40f, 1.00f));
        mat.set("uIntensity", 1.0f);
        mat.setQueue(RenderQueue.TRANSPARENT);
        return mat;
    }

    /**
     * Orients the flat swirl quad so its normal is perpendicular to the travel
     * direction {@code d} (and flipped for the opposite face).
     */
    private static Matrix4f swirlTransform(Vector3f pos, Snake.Dir d,
                                           float scale, boolean flip) {
        float s = flip ? -1f : 1f;
        float tx = d.dx * s;
        float ty = d.dy * s;
        float tz = d.dz * s;

        Matrix4f m = new Matrix4f().translation(pos);
        if (tx > 0) {
            m.rotateZ((float) (-Math.PI / 2));
        } else if (tx < 0) {
            m.rotateZ((float) (Math.PI / 2));
        } else if (tz > 0) {
            m.rotateX((float) (Math.PI / 2));
        } else if (tz < 0) {
            m.rotateX((float) (-Math.PI / 2));
        } else if (ty < 0) {
            m.rotateX((float) Math.PI);
        }
        return m.scale(scale, scale, 1f);
    }

    private static Vector3f headWorld(Snake snake, float t) {
        List<Snake.Cell> cur = snake.body();
        List<Snake.Cell> prev = snake.prevBody();
        Snake.Cell hc = cur.get(0);
        Snake.Cell hp = prev.isEmpty() ? hc : prev.get(0);
        float tt = Math.min(Math.max(t, 0f), 1f);
        return new Vector3f(
                wrap(lerp(hp.x() + 0.5f, hc.x() + 0.5f, tt), SIZE) - SIZE / 2f,
                wrap(lerp(hp.y() + 0.5f, hc.y() + 0.5f, tt), SIZE) - SIZE / 2f,
                wrap(lerp(hp.z() + 0.5f, hc.z() + 0.5f, tt), SIZE) - SIZE / 2f);
    }

    private static float minDistToSnake(Snake snake, Vector3f p) {
        float best = Float.MAX_VALUE;
        for (Snake.Cell c : snake.body()) {
            float wx = wrap(c.x() + 0.5f, SIZE) - SIZE / 2f;
            float wy = wrap(c.y() + 0.5f, SIZE) - SIZE / 2f;
            float wz = wrap(c.z() + 0.5f, SIZE) - SIZE / 2f;
            float dx = wx - p.x;
            float dy = wy - p.y;
            float dz = wz - p.z;
            best = Math.min(best, dx * dx + dy * dy + dz * dz);
        }
        return (float) Math.sqrt(best);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float wrap(float v, float size) {
        float m = v % size;
        return m < 0 ? m + size : m;
    }

    private static Material material(ShaderProgram shader, float r, float g, float b) {
        Material mat = new Material(shader);
        mat.set("uColor", new Vector3f(r, g, b));
        return mat;
    }

    private static Camera buildCamera(Window win) {
        float aspect = (float) win.getWidth() / (float) win.getHeight();
        PerspectiveProjection proj = new PerspectiveProjection(50f, aspect, 0.01f);
        return new Camera(new Matrix4f(), proj.get(new Matrix4f()));
    }

    private static void updateCamera(Camera camera, Vector3f target,
                                     float yaw, float pitch, float radius) {
        float cp = (float) Math.cos(Math.toRadians(pitch));
        float sp = (float) Math.sin(Math.toRadians(pitch));
        float cy = (float) Math.cos(Math.toRadians(yaw));
        float sy = (float) Math.sin(Math.toRadians(yaw));
        Vector3f eye = new Vector3f(
                target.x + radius * cp * sy,
                target.y + radius * sp,
                target.z + radius * cp * cy);
        camera.view.identity().lookAt(eye, target, new Vector3f(0f, 1f, 0f));
    }
}

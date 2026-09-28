package com.melon.snake3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Pure game logic for a 3D snake on an {@code sx x sy x sz} cube, with no engine
 * dependency. {@link #step()} advances one tick.
 *
 * <p>The body stores <b>unbounded</b> grid coordinates so a portal wrap is just
 * an ordinary one-cell move that leaves {@code [0, size)}. Logic reads wrapped
 * coordinates; rendering interpolates between {@link #prevBody()} and
 * {@link #body()} for smooth motion.</p>
 */
public final class Snake {

    public enum Dir {
        NORTH(0, 0, -1), SOUTH(0, 0, 1),
        WEST(-1, 0, 0), EAST(1, 0, 0),
        UP(0, 1, 0), DOWN(0, -1, 0);

        public final int dx;
        public final int dy;
        public final int dz;

        Dir(int dx, int dy, int dz) {
            this.dx = dx;
            this.dy = dy;
            this.dz = dz;
        }
    }

    public record Cell(int x, int y, int z) {
    }

    private final int sx;
    private final int sy;
    private final int sz;
    private final boolean[][][] occupied;
    private final Deque<Cell> body = new ArrayDeque<>();
    private Deque<Cell> prevBody = new ArrayDeque<>();
    private final Deque<Dir> dirQueue = new ArrayDeque<>();
    private final List<Cell> foods = new ArrayList<>();
    private final Random rng = new Random();

    private Dir dir;
    private Dir wrapDir = null;
    private int targetFoodCount = 1;
    private boolean alive = true;
    private boolean won = false;
    private int score;

    public Snake(int sx, int sy, int sz) {
        this.sx = sx;
        this.sy = sy;
        this.sz = sz;
        this.occupied = new boolean[sx][sy][sz];

        int cx = sx / 2;
        int cy = sy / 2;
        int cz = sz / 2;
        body.addFirst(new Cell(cx, cy, cz));
        prevBody.addFirst(new Cell(cx, cy, cz));
        occupied[cx][cy][cz] = true;
        dir = Dir.EAST;
        refillFoods();
    }

    /**
     * Queues a direction change. Reversals (direct 180-degree turns) are
     * rejected; up to two turns are buffered so rapid key presses are not lost.
     */
    public void setDirection(Dir d) {
        Dir last = dirQueue.isEmpty() ? dir : dirQueue.peekLast();
        if (dirQueue.size() >= 2 || d == last || d == opposite(last)) {
            return;
        }
        dirQueue.addLast(d);
    }

    /** Sets how many food items should be on the board (minimum 1). */
    public void setFoodCount(int count) {
        targetFoodCount = Math.max(1, count);
        refillFoods();
    }

    public int foodCount() {
        return targetFoodCount;
    }

    public void step() {
        if (!alive) {
            return;
        }
        if (!dirQueue.isEmpty()) {
            dir = dirQueue.removeFirst();
        }

        normalize();
        prevBody = new ArrayDeque<>(body);

        Cell head = body.peekFirst();
        int nx = head.x() + dir.dx;
        int ny = head.y() + dir.dy;
        int nz = head.z() + dir.dz;
        int wx = wrap(nx, sx);
        int wy = wrap(ny, sy);
        int wz = wrap(nz, sz);

        wrapDir = (nx != wx || ny != wy || nz != wz) ? dir : null;

        boolean eating = false;
        int eaten = -1;
        for (int i = 0; i < foods.size(); i++) {
            Cell f = foods.get(i);
            if (f.x() == wx && f.y() == wy && f.z() == wz) {
                eating = true;
                eaten = i;
                break;
            }
        }

        if (!eating && occupied[wx][wy][wz]) {
            Cell tail = body.peekLast();
            boolean intoTail = wrap(tail.x(), sx) == wx
                    && wrap(tail.y(), sy) == wy
                    && wrap(tail.z(), sz) == wz;
            if (!intoTail) {
                alive = false;
                return;
            }
        }

        body.addFirst(new Cell(nx, ny, nz));
        occupied[wx][wy][wz] = true;

        if (eating) {
            foods.remove(eaten);
            score++;
            refillFoods();
        } else {
            Cell tail = body.removeLast();
            occupied[wrap(tail.x(), sx)][wrap(tail.y(), sy)][wrap(tail.z(), sz)] = false;
        }
    }

    /**
     * Shifts every segment by the same multiple of each dimension so the head
     * lands back in {@code [0, size)}. Keeps the unbounded coordinates bounded
     * without changing any segment's wrapped position or its neighbour.
     */
    private void normalize() {
        Cell head = body.peekFirst();
        int ox = Math.floorDiv(head.x(), sx) * sx;
        int oy = Math.floorDiv(head.y(), sy) * sy;
        int oz = Math.floorDiv(head.z(), sz) * sz;
        if (ox == 0 && oy == 0 && oz == 0) {
            return;
        }
        Deque<Cell> norm = new ArrayDeque<>(body.size());
        for (Cell c : body) {
            norm.addLast(new Cell(c.x() - ox, c.y() - oy, c.z() - oz));
        }
        body.clear();
        body.addAll(norm);
    }

    private void refillFoods() {
        while (foods.size() < targetFoodCount) {
            Cell c = randomFreeCell();
            if (c == null) {
                if (foods.isEmpty() && body.size() >= sx * sy * sz) {
                    won = true;
                    alive = false;
                }
                return;
            }
            foods.add(c);
        }
    }

    private Cell randomFreeCell() {
        List<Cell> free = new ArrayList<>();
        for (int x = 0; x < sx; x++) {
            for (int y = 0; y < sy; y++) {
                for (int z = 0; z < sz; z++) {
                    if (occupied[x][y][z] || isFood(x, y, z)) {
                        continue;
                    }
                    free.add(new Cell(x, y, z));
                }
            }
        }
        if (free.isEmpty()) {
            return null;
        }
        return free.get(rng.nextInt(free.size()));
    }

    private boolean isFood(int x, int y, int z) {
        for (Cell f : foods) {
            if (f.x() == x && f.y() == y && f.z() == z) {
                return true;
            }
        }
        return false;
    }

    /** @return current unbounded segments, head first (post-step) */
    public List<Cell> body() {
        return new ArrayList<>(body);
    }

    /** @return unbounded segments before the last step, head first */
    public List<Cell> prevBody() {
        return new ArrayList<>(prevBody);
    }

    /** @return true if the most recent step wrapped the head through a portal */
    public boolean headWrapped() {
        return wrapDir != null;
    }

    /** @return the direction of the most recent wrap, or null */
    public Dir wrapDir() {
        return wrapDir;
    }

    /** @return the current unbounded head cell */
    public Cell head() {
        return body.peekFirst();
    }

    /** @return the current food items (wrapped coordinates) */
    public List<Cell> foods() {
        return new ArrayList<>(foods);
    }

    public boolean isAlive() {
        return alive;
    }

    public boolean won() {
        return won;
    }

    public int score() {
        return score;
    }

    public int length() {
        return body.size();
    }

    private static int wrap(int v, int size) {
        int m = v % size;
        return m < 0 ? m + size : m;
    }

    private static Dir opposite(Dir d) {
        return switch (d) {
            case NORTH -> Dir.SOUTH;
            case SOUTH -> Dir.NORTH;
            case WEST -> Dir.EAST;
            case EAST -> Dir.WEST;
            case UP -> Dir.DOWN;
            case DOWN -> Dir.UP;
        };
    }
}

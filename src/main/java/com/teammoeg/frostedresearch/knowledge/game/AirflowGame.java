package com.teammoeg.frostedresearch.knowledge.game;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.BitSet;

/** Dig a wind channel into the furnace's charcoal bed. The fixed walls make the bed unavoidable. */
public final class AirflowGame {
    public static final int WIDTH = 28, HEIGHT = 28, SIZE = WIDTH * HEIGHT;
    public static final int SOIL = 0, OPEN = 1, WALL = 2, BED = 3;
    public static final int INLET = 20 * WIDTH, OUTLET = 18;
    public record Stroke(int from, int to) {}

    private final BitSet dug = new BitSet(SIZE);
    private int[] path = new int[0];
    private boolean completed;

    public static int terrain(int cell) {
        if (cell < 0 || cell >= SIZE) return WALL;
        int x = cell % WIDTH, y = cell / WIDTH;
        if (y <= 13 && ((x >= 13 && x <= 14) || (x >= 22 && x <= 23))) return WALL;
        if (y <= 13 && x >= 15 && x <= 21) return y >= 12 ? BED : OPEN;
        if (x <= 2 && y >= 19 && y <= 21) return OPEN;
        return SOIL;
    }

    public int cell(int cell) { return dug.get(cell) ? OPEN : terrain(cell); }
    public boolean won() { return completed; }
    public void restoreCompletion() { completed = true; }
    public void reset() { dug.clear(); path = new int[0]; completed = false; }

    /** Three-cell diamond brush; interpolate the whole stroke so fast dragging leaves no gaps. */
    public boolean dig(int from, int to) {
        if (completed || from < 0 || from >= SIZE || to < 0 || to >= SIZE) return false;
        int x = from % WIDTH, y = from / WIDTH, tx = to % WIDTH, ty = to / WIDTH;
        int dx = Math.abs(tx - x), dy = -Math.abs(ty - y);
        int sx = x < tx ? 1 : -1, sy = y < ty ? 1 : -1, error = dx + dy;
        boolean changed = false;
        while (true) {
            for (int oy = -1; oy <= 1; oy++) for (int ox = -1; ox <= 1; ox++) {
                if (Math.abs(ox) + Math.abs(oy) > 1) continue;
                int bx = x + ox, by = y + oy;
                if (bx < 0 || bx >= WIDTH || by < 0 || by >= HEIGHT) continue;
                int cell = by * WIDTH + bx;
                if (terrain(cell) == SOIL && !dug.get(cell)) { dug.set(cell); changed = true; }
            }
            if (x == tx && y == ty) break;
            int e = 2 * error;
            if (e >= dy) { error += dy; x += sx; }
            if (e <= dx) { error += dx; y += sy; }
        }
        if (changed) findPath();
        return changed;
    }

    private void findPath() {
        int[] parent = new int[SIZE]; Arrays.fill(parent, -1);
        var queue = new ArrayDeque<Integer>(); queue.add(INLET); parent[INLET] = INLET;
        while (!queue.isEmpty() && parent[OUTLET] == -1) {
            int at = queue.remove(), x = at % WIDTH, y = at / WIDTH;
            for (int next : new int[]{x > 0 ? at - 1 : -1, x + 1 < WIDTH ? at + 1 : -1,
                y > 0 ? at - WIDTH : -1, y + 1 < HEIGHT ? at + WIDTH : -1}) {
                if (next < 0 || parent[next] != -1 || cell(next) == SOIL || cell(next) == WALL) continue;
                parent[next] = at; queue.add(next);
            }
        }
        path = new int[0];
        if (parent[OUTLET] == -1) return;
        int length = 1;
        for (int at = OUTLET; at != INLET; at = parent[at]) length++;
        path = new int[length];
        for (int at = OUTLET, i = length - 1; i >= 0; i--, at = parent[at]) path[i] = at;
        completed = true;
    }

    public CompoundTag save() {
        var tag = new CompoundTag(); tag.putInt("format", 2);
        tag.putIntArray("dug", dug.stream().toArray()); tag.putBoolean("completed", completed); return tag;
    }

    public void load(CompoundTag tag) {
        reset();
        // Rotating-pipe saves have no excavation to preserve; research completion is reconciled by the desk.
        if (tag.getInt("format") != 2) return;
        for (int cell : tag.getIntArray("dug")) if (cell >= 0 && cell < SIZE && terrain(cell) == SOIL) dug.set(cell);
        findPath(); completed |= tag.getBoolean("completed");
    }

    public CompoundTag view() {
        var tag = save(); int[] cells = new int[SIZE];
        for (int i = 0; i < SIZE; i++) cells[i] = cell(i);
        tag.putIntArray("cells", cells); tag.putIntArray("path", path); tag.putBoolean("won", completed); return tag;
    }
}

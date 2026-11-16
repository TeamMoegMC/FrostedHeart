package com.teammoeg.frostedresearch.knowledge.game;

import net.minecraft.nbt.CompoundTag;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/** Carve sand bricks; a cold-air escape takes priority over a route through the charcoal bed. */
public final class AirflowGame {
    public static final int WIDTH = 28, HEIGHT = 28, SIZE = WIDTH * HEIGHT;
    public static final int BRICK = 0, OPEN = 1, PROTECTED = 2, BED = 3;
    public static final int INLET = 20 * WIDTH, OUTLET = 18;
    public record Stroke(int from, int to) {}

    private final BitSet dug = new BitSet(SIZE);
    private int[] path = new int[0];
    private int leak = -1;
    private boolean completed, failed;

    public static int terrain(int cell) {
        if (cell < 0 || cell >= SIZE) return PROTECTED;
        int x = cell % WIDTH, y = cell / WIDTH;
        if ((x == 0 && y >= 19 && y <= 21) || (x >= 1 && x <= 4 && y >= 18 && y <= 22)) return OPEN;
        if (x <= 1 && y >= 16 && y <= 24) return PROTECTED;
        if (y <= 13 && x >= 15 && x <= 21) return y >= 12 ? BED : OPEN;
        return BRICK;
    }
    public static boolean boundary(int cell) {
        int x = cell % WIDTH, y = cell / WIDTH;
        return x == 0 || x == WIDTH - 1 || y == 0 || y == HEIGHT - 1;
    }
    public static boolean inletOpening(int cell) { return cell % WIDTH == 0 && cell / WIDTH >= 19 && cell / WIDTH <= 21; }
    public static boolean chimneyOpening(int cell) { return cell / WIDTH == 0 && cell % WIDTH >= 15 && cell % WIDTH <= 21; }
    public int cell(int cell) { return cell >= 0 && cell < SIZE && dug.get(cell) ? OPEN : terrain(cell); }
    public int dugCount() { return dug.cardinality(); }
    public boolean won() { return completed; }
    public boolean failed() { return failed; }
    public void restoreCompletion() { completed = true; failed = false; leak = -1; }
    public void reset() { dug.clear(); path = new int[0]; completed = failed = false; leak = -1; }
    public boolean dig(int from, int to) { return dig(List.of(new Stroke(from, to))); }

    /** Evaluate once after the whole packet: a later breach in the same batch must not be hidden by an earlier win. */
    public boolean dig(List<Stroke> strokes) {
        if (completed || failed) return false;
        boolean changed = false;
        for (var stroke : strokes) changed |= carve(stroke.from(), stroke.to());
        if (changed) evaluate();
        return changed;
    }
    private boolean carve(int from, int to) {
        if (from < 0 || from >= SIZE || to < 0 || to >= SIZE) return false;
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
                if (terrain(cell) == BRICK && !dug.get(cell)) { dug.set(cell); changed = true; }
            }
            if (x == tx && y == ty) break;
            int e = 2 * error;
            if (e >= dy) { error += dy; x += sx; }
            if (e <= dx) { error += dx; y += sy; }
        }
        return changed;
    }
    private int[] search(boolean coldOnly) {
        int[] parent = new int[SIZE]; Arrays.fill(parent, -1);
        var queue = new ArrayDeque<Integer>(); queue.add(INLET); parent[INLET] = INLET;
        while (!queue.isEmpty()) {
            int at = queue.remove(), x = at % WIDTH, y = at / WIDTH;
            if (coldOnly && boundary(at) && !inletOpening(at)) { leak = at; return parent; }
            for (int next : new int[]{x > 0 ? at - 1 : -1, x + 1 < WIDTH ? at + 1 : -1,
                y > 0 ? at - WIDTH : -1, y + 1 < HEIGHT ? at + WIDTH : -1}) {
                if (next < 0 || parent[next] != -1) continue;
                int type = cell(next);
                if (type == BRICK || type == PROTECTED || (coldOnly && type == BED)) continue;
                parent[next] = at; queue.add(next);
            }
        }
        return parent;
    }
    private void evaluate() {
        path = new int[0]; leak = -1; failed = false;
        search(true);
        if (leak >= 0) { failed = true; return; }
        int[] parent = search(false);
        if (parent[OUTLET] == -1) return;
        int length = 1;
        for (int at = OUTLET; at != INLET; at = parent[at]) length++;
        path = new int[length];
        for (int at = OUTLET, i = length - 1; i >= 0; i--, at = parent[at]) path[i] = at;
        completed = true;
    }
    public CompoundTag save() {
        var tag = new CompoundTag(); tag.putInt("format", 3);
        tag.putIntArray("dug", dug.stream().toArray()); tag.putBoolean("completed", completed); tag.putBoolean("failed", failed); return tag;
    }
    public void load(CompoundTag tag) {
        reset();
        int format = tag.getInt("format");
        if (format != 2 && format != 3) return;
        for (int cell : tag.getIntArray("dug")) if (cell >= 0 && cell < SIZE && terrain(cell) == BRICK) dug.set(cell);
        evaluate();
        if (tag.getBoolean("completed")) restoreCompletion();
    }
    public CompoundTag view() {
        var tag = save(); int[] cells = new int[SIZE];
        for (int i = 0; i < SIZE; i++) cells[i] = cell(i);
        tag.putIntArray("cells", cells); tag.putIntArray("path", path); tag.putBoolean("won", completed); tag.putInt("leak", leak); return tag;
    }
}

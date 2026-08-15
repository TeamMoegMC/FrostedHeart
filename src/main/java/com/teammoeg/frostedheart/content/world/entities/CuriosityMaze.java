/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.world.entities;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * 雪墙迷宫的确定性生成器（递归回溯）。
 * <p>
 * Deterministic snow-wall maze generator (recursive backtracker). The layout
 * only depends on the RandomSource seed, so a maze can be regenerated after a
 * server restart. Cell size is 3 blocks, walls are 1 block thick:
 * footprint = cells * (CELL_SIZE + 1) - 1.
 */
public class CuriosityMaze {
    public static final int CELL_SIZE = 3;
    public static final int STRIDE = CELL_SIZE + 1;

    public final int cells;
    public final int footprint;

    /** 相邻两格之间竖墙（x 方向）是否闭合 / Vertical walls between cells (x direction). */
    private boolean[][] vWall;
    /** 相邻两格之间横墙（z 方向）是否闭合 / Horizontal walls between cells (z direction). */
    private boolean[][] hWall;
    private BlockPos origin = BlockPos.ZERO;
    private int entranceCX, entranceCZ;
    private int borderSide;
    private int coreCX, coreCZ;

    public CuriosityMaze(int cells) {
        this.cells = cells;
        this.footprint = cells * STRIDE - 1;
    }

    /** 生成迷宫 / Generates the maze from the given seed. */
    public void generate(RandomSource random) {
        this.vWall = new boolean[cells - 1][cells];
        this.hWall = new boolean[cells][cells - 1];
        for (boolean[] a : vWall) Arrays.fill(a, true);
        for (boolean[] a : hWall) Arrays.fill(a, true);
        boolean[][] visited = new boolean[cells][cells];
        Deque<int[]> stack = new ArrayDeque<>();
        int sx = random.nextInt(cells), sz = random.nextInt(cells);
        visited[sx][sz] = true;
        stack.push(new int[] { sx, sz });
        while (!stack.isEmpty()) {
            int[] c = stack.peek();
            List<int[]> next = new ArrayList<>();
            if (c[0] + 1 < cells && !visited[c[0] + 1][c[1]]) next.add(new int[] { c[0] + 1, c[1] });
            if (c[0] - 1 >= 0 && !visited[c[0] - 1][c[1]]) next.add(new int[] { c[0] - 1, c[1] });
            if (c[1] + 1 < cells && !visited[c[0]][c[1] + 1]) next.add(new int[] { c[0], c[1] + 1 });
            if (c[1] - 1 >= 0 && !visited[c[0]][c[1] - 1]) next.add(new int[] { c[0], c[1] - 1 });
            if (next.isEmpty()) {
                stack.pop();
                continue;
            }
            int[] n = next.get(random.nextInt(next.size()));
            int dx = n[0] - c[0], dz = n[1] - c[1];
            if (dx == 1) vWall[c[0]][c[1]] = false;
            else if (dx == -1) vWall[n[0]][n[1]] = false;
            else if (dz == 1) hWall[c[0]][c[1]] = false;
            else hWall[n[0]][n[1]] = false;
            visited[n[0]][n[1]] = true;
            stack.push(n);
        }
    }

    public void setOrigin(BlockPos origin) {
        this.origin = origin;
    }

    public BlockPos origin() {
        return this.origin;
    }

    /**
     * 以世界坐标设置入口（升起时刻玩家所在格），并选择最近的边界开口。
     * <p>
     * Sets the entrance from world coordinates (the cell containing the player
     * when the maze rises) and picks the nearest border opening.
     */
    public void setEntrance(int worldX, int worldZ) {
        int lx = Mth.clamp(worldX - origin.getX(), 0, footprint - 1);
        int lz = Mth.clamp(worldZ - origin.getZ(), 0, footprint - 1);
        entranceCX = cellIndex(lx);
        entranceCZ = cellIndex(lz);
        int dN = lz, dS = footprint - 1 - lz, dW = lx, dE = footprint - 1 - lx;
        borderSide = 0;
        int best = dN;
        if (dS < best) { best = dS; borderSide = 1; }
        if (dW < best) { best = dW; borderSide = 2; }
        if (dE < best) { best = dE; borderSide = 3; }
    }

    /** 恢复入口（NBT 载入用）/ Restores the entrance (NBT reload). */
    public void setEntranceCell(int cx, int cz, int side) {
        this.entranceCX = Mth.clamp(cx, 0, cells - 1);
        this.entranceCZ = Mth.clamp(cz, 0, cells - 1);
        this.borderSide = Mth.clamp(side, 0, 3);
    }

    public int entranceCellX() {
        return entranceCX;
    }

    public int entranceCellZ() {
        return entranceCZ;
    }

    public int borderSide() {
        return borderSide;
    }

    private int cellIndex(int l) {
        return Mth.clamp((l - 1) / STRIDE, 0, cells - 1);
    }

    /** 从距入口最远的格中随机选核心格 / Chooses the core cell among the farthest cells. */
    public void chooseCore(RandomSource random) {
        int[][] dist = bfs(entranceCX, entranceCZ);
        int max = 0;
        for (int[] row : dist) {
            for (int d : row) max = Math.max(max, d);
        }
        List<int[]> candidates = new ArrayList<>();
        for (int x = 0; x < cells; x++) {
            for (int z = 0; z < cells; z++) {
                if (dist[x][z] == max) candidates.add(new int[] { x, z });
            }
        }
        int[] c = candidates.get(random.nextInt(candidates.size()));
        coreCX = c[0];
        coreCZ = c[1];
    }

    /** 恢复核心格（NBT 载入用）/ Restores the core cell (NBT reload). */
    public void setCoreCell(int cx, int cz) {
        this.coreCX = Mth.clamp(cx, 0, cells - 1);
        this.coreCZ = Mth.clamp(cz, 0, cells - 1);
    }

    public int coreCellX() {
        return coreCX;
    }

    public int coreCellZ() {
        return coreCZ;
    }

    /** 核心格中心的世界坐标（y 由调用方补）/ Core cell center world position (caller sets y). */
    public BlockPos coreWorldPos() {
        return origin.offset(coreCX * STRIDE + 2, 0, coreCZ * STRIDE + 2);
    }

    /**
     * 按与入口的 BFS 距离排序的墙柱列表（用于波次升起动画）。
     * <p>
     * Wall columns ordered by BFS distance from the entrance, for the wave
     * rising animation.
     */
    public List<BlockPos> orderedWallColumns(RandomSource random) {
        int[][] dist = bfs(entranceCX, entranceCZ);
        List<BlockPos> cols = new ArrayList<>();
        for (int lx = 0; lx < footprint; lx++) {
            for (int lz = 0; lz < footprint; lz++) {
                if (!isWallColumn(lx, lz)) continue;
                cols.add(origin.offset(lx, 0, lz));
            }
        }
        cols.sort(Comparator.comparingInt(p -> dist[Mth.clamp((p.getX() - origin.getX()) / STRIDE, 0, cells - 1)]
                [Mth.clamp((p.getZ() - origin.getZ()) / STRIDE, 0, cells - 1)]));
        return cols;
    }

    /** 该列是否为墙（含边界与通道开口判定）/ Whether this column is a wall. */
    private boolean isWallColumn(int lx, int lz) {
        boolean border = lx == 0 || lx == footprint - 1 || lz == 0 || lz == footprint - 1;
        boolean vLine = lx > 0 && lx < footprint - 1 && lx % STRIDE == 0;
        boolean hLine = lz > 0 && lz < footprint - 1 && lz % STRIDE == 0;
        if (!border && !vLine && !hLine) return false;
        // 入口开口（边界上 3 格宽的缺口）/ entrance opening on the nearest border
        if (border && borderSide == 0 && lz == 0
                && lx >= entranceCX * STRIDE + 1 && lx <= entranceCX * STRIDE + 3) return false;
        if (border && borderSide == 1 && lz == footprint - 1
                && lx >= entranceCX * STRIDE + 1 && lx <= entranceCX * STRIDE + 3) return false;
        if (border && borderSide == 2 && lx == 0
                && lz >= entranceCZ * STRIDE + 1 && lz <= entranceCZ * STRIDE + 3) return false;
        if (border && borderSide == 3 && lx == footprint - 1
                && lz >= entranceCZ * STRIDE + 1 && lz <= entranceCZ * STRIDE + 3) return false;
        // 通道开口 / passage openings
        if (vLine) {
            int vx = lx / STRIDE - 1;
            int vz = Mth.clamp(lz / STRIDE, 0, cells - 1);
            if (!vWall[vx][vz] && lz == vz * STRIDE + 2) return false;
        }
        if (hLine) {
            int hz = lz / STRIDE - 1;
            int hx = Mth.clamp(lx / STRIDE, 0, cells - 1);
            if (!hWall[hx][hz] && lx == hx * STRIDE + 2) return false;
        }
        return true;
    }

    private int[][] bfs(int sx, int sz) {
        int[][] dist = new int[cells][cells];
        for (int[] row : dist) Arrays.fill(row, -1);
        Deque<int[]> q = new ArrayDeque<>();
        dist[sx][sz] = 0;
        q.add(new int[] { sx, sz });
        while (!q.isEmpty()) {
            int[] c = q.poll();
            int d = dist[c[0]][c[1]] + 1;
            if (c[0] + 1 < cells && !vWall[c[0]][c[1]] && dist[c[0] + 1][c[1]] < 0) {
                dist[c[0] + 1][c[1]] = d;
                q.add(new int[] { c[0] + 1, c[1] });
            }
            if (c[0] - 1 >= 0 && !vWall[c[0] - 1][c[1]] && dist[c[0] - 1][c[1]] < 0) {
                dist[c[0] - 1][c[1]] = d;
                q.add(new int[] { c[0] - 1, c[1] });
            }
            if (c[1] + 1 < cells && !hWall[c[0]][c[1]] && dist[c[0]][c[1] + 1] < 0) {
                dist[c[0]][c[1] + 1] = d;
                q.add(new int[] { c[0], c[1] + 1 });
            }
            if (c[1] - 1 >= 0 && !hWall[c[0]][c[1] - 1] && dist[c[0]][c[1] - 1] < 0) {
                dist[c[0]][c[1] - 1] = d;
                q.add(new int[] { c[0], c[1] - 1 });
            }
        }
        return dist;
    }
}

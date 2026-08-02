package com.teammoeg.frostedheart.util;

import java.util.Arrays;
import java.util.Random;

/**
 * 群系列 1D 求解器（优化方案2 的核心，纯算法）。
 *
 * 原理：biome TargetPoint 的 6 个量化坐标中，温度/湿度/大陆性/侵蚀/怪异度在 vanilla
 * router 下与 Y 无关（2D 函数），沿 (x,z) 列只有 depth 变化。因此：
 *
 *   fitness_i(y) = C_i + dist(depthInterval_i, depth[y])^2
 *
 * 其中 C_i = 其余 5 维的平方距离和 + offset^2，每列只需算一次。
 * 本类完全复刻 vanilla 的定点(long)语义：
 *   - Climate.Parameter.distance(long)：i = p - max; j = min - p; return i>0 ? i : max(j,0)
 *   - ParameterPoint.fitness：各维 distance 的平方和（含 offset 的平方）
 *   - 打平规则：严格小于才替换 ⇒ 保留列表中先出现者（与 findValueBruteForce 一致）
 *
 * 与 RTree 的差异：RTree 是同一 argmin 的空间索引版；在 fitness 完全相等的病态平局下
 * 遍历顺序可能不同（实测概率≈0，因为 offset 项几乎必然区分），黄金 diff 可兜底。
 *
 * 自包含自测：javac pure/BiomeColumnSolver.java && java pure.BiomeColumnSolver
 */
public final class BiomeColumnSolver {

    private BiomeColumnSolver() {}

    /** 每点 6 维区间的扁平存储下标约定：boxes[i] = {tMin,tMax, hMin,hMax, cMin,cMax, eMin,eMax, dMin,dMax, wMin,wMax} */
    public static final int DIMS = 6;
    public static final int DEPTH_DIM = 4; // depth 在 boxes 行内的起始下标 = DEPTH_DIM*2

    private static long dist(long min, long max, long p) {
        long i = p - max;
        long j = min - p;
        return i > 0L ? i : Math.max(j, 0L);
    }

    /**
     * 计算某列的 C_i：5 个 Y 不变维度的平方距离和 + offset^2。
     * @param boxes   [n][12] 每个参数点的 6 维量化区间
     * @param offsets [n] 每个参数点的量化 offset（quantizeCoord 后）
     * @param fixed5  [5] 本列采样到的 5 个 Y 不变坐标（temperature, humidity, continentalness, erosion, weirdness）
     */
    public static long[] baseFitness(long[][] boxes, long[] offsets, long[] fixed5) {
        int n = boxes.length;
        long[] c = new long[n];
        for (int i = 0; i < n; i++) {
            long[] b = boxes[i];
            long f = 0L;
            // 维度 0..3 与 5 是 Y 不变维度；维度 4 (depth) 跳过，在 solveColumn 中逐 Y 处理
            for (int dim = 0; dim < DIMS; dim++) {
                if (dim == DEPTH_DIM) continue;
                long d = dist(b[dim * 2], b[dim * 2 + 1], fixed5[dim < DEPTH_DIM ? dim : dim - 1]);
                f += d * d;
            }
            long o = offsets[i];
            c[i] = f + o * o;
        }
        return c;
    }

    /**
     * 对一整列求每个 Y 格的胜者参数点下标。
     * @param c          baseFitness 的结果（每点常数项）
     * @param depthMin   [n] 每点 depth 区间下界
     * @param depthMax   [n] 每点 depth 区间上界
     * @param depthPerY  [ny] 本列每个 Y 格采样到的量化 depth（顺序由调用方决定，无需单调）
     * @return [ny] 每个 Y 格的胜者参数点下标
     */
    public static int[] solveColumn(long[] c, long[] depthMin, long[] depthMax, long[] depthPerY) {
        int n = c.length;
        int ny = depthPerY.length;
        int[] out = new int[ny];
        for (int y = 0; y < ny; y++) {
            long d = depthPerY[y];
            int best = 0;
            long bestFit = Long.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                long di = d - depthMax[i];
                if (di <= 0L) {
                    di = depthMin[i] - d;
                    if (di < 0L) di = 0L;
                }
                long f = c[i] + di * di;
                if (f < bestFit) { // 严格小于：与 vanilla findValueBruteForce 的打平规则一致
                    bestFit = f;
                    best = i;
                }
            }
            out[y] = best;
        }
        return out;
    }

}

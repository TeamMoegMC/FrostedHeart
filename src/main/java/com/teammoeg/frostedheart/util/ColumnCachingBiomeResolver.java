package com.teammoeg.frostedheart.util;

import com.mojang.datafixers.util.Pair;
import java.util.List;

import com.teammoeg.frostedheart.mixin.minecraft.accessors.MultiNoiseBiomeSourceAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.DensityFunction;


/**
 * 群系列缓存 BiomeResolver（优化方案2的接线层，Mojmap 1.20.1）。
 *
 * 接线方式：见 ChunkAccessBiomeMixin —— 用 @ModifyArg 把传给
 * ChunkAccess#fillBiomesFromNoise 的 BiomeResolver 换成本类。
 * 无需触碰 LevelChunkSection 的 palette 内部，也不与 FastNoise 的
 * @Redirect 冲突（FastNoise 存在时，它的 FastWorldgen/FastBiomeGen
 * 会拿着本包装类逐格取值，两者链式叠加）。
 *
 * 每 (x,z) 四分位列成本：
 *   1 次完整 TargetPoint 采样（拿 5 个 Y 不变坐标）
 * + 1 次列顶复采校验（守护非 vanilla router 的 datapack，5 坐标不一致则整列回退原版）
 * + countY 次 depth 单函数采样（替代 vanilla 每格 6 函数全采样 + RTree 搜索）
 * + 1 次 BiomeColumnSolver.solveColumn（无分配扁平扫描，语义逐位一致）
 *
 * 依赖 MultiNoiseBiomeSourceAccessor（Mixin）读取私有 parameters()。
 * 本类对 BiomeSupplier/BiomeResolver 的调用顺序不敏感：vanilla 与
 * FastNoise 的 FastBiomeGen 遍历顺序不同，但列缓存按 (x,z) 惰性填充，均正确。
 */
public final class ColumnCachingBiomeResolver implements BiomeResolver {

    private static final Holder<Biome>[] FALLBACK = new Holder[0];

    private final BiomeResolver fallback;                 // 一般是 biomeSource 本身
    private final Climate.Sampler sampler;
    private final DensityFunction depthFn;
    private final long[][] boxes;                         // [n][12]，布局同 BiomeColumnSolver
    private final long[] offsets;
    private final long[] depthMin, depthMax;
    private final Holder<Biome>[] biomes;
    private final int firstQuartX, firstQuartZ, minQuartY, countY;
    private final Holder<Biome>[][] columns = new Holder[16][];
    private final MutableContext ctx = new MutableContext();

    @SuppressWarnings("unchecked")
    public ColumnCachingBiomeResolver(MultiNoiseBiomeSource source, Climate.Sampler sampler, ChunkAccess chunk) {
        this.fallback = source;
        this.sampler = sampler;
        this.depthFn = sampler.depth();

        List<Pair<Climate.ParameterPoint, Holder<Biome>>> values =
                ((MultiNoiseBiomeSourceAccessor) source).fast$parameters().values();
        int n = values.size();
        this.boxes = new long[n][12];
        this.offsets = new long[n];
        this.depthMin = new long[n];
        this.depthMax = new long[n];
        this.biomes = new Holder[n];
        for (int i = 0; i < n; i++) {
            Climate.ParameterPoint p = values.get(i).getFirst();
            put(boxes[i], 0, p.temperature());
            put(boxes[i], 2, p.humidity());
            put(boxes[i], 4, p.continentalness());
            put(boxes[i], 6, p.erosion());
            put(boxes[i], 8, p.depth());
            put(boxes[i], 10, p.weirdness());
            this.offsets[i] = p.offset();
            this.depthMin[i] = p.depth().min();
            this.depthMax[i] = p.depth().max();
            this.biomes[i] = values.get(i).getSecond();
        }

        this.firstQuartX = QuartPos.fromBlock(chunk.getPos().getMinBlockX());
        this.firstQuartZ = QuartPos.fromBlock(chunk.getPos().getMinBlockZ());
        this.minQuartY = QuartPos.fromBlock(chunk.getMinBuildHeight());
        this.countY = chunk.getSections().length * 4; // 每个 section 16 格 = 4 个四分位格
    }

    private static void put(long[] row, int idx, Climate.Parameter p) {
        row[idx] = p.min();
        row[idx + 1] = p.max();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int qx, int qy, int qz, Climate.Sampler s) {
        int lx = qx - firstQuartX, lz = qz - firstQuartZ, ly = qy - minQuartY;
        if (lx < 0 || lx > 3 || lz < 0 || lz > 3 || ly < 0 || ly >= countY) {
            return fallback.getNoiseBiome(qx, qy, qz, s); // 区块边界外（fillBiomesFromNoise 不会越界，防御）
        }
        Holder<Biome>[] col = columns[(lx << 2) | lz];
        if (col == null) {
            col = buildColumn(qx, qz);
            columns[(lx << 2) | lz] = col;
        }
        if (col == FALLBACK) {
            return fallback.getNoiseBiome(qx, qy, qz, s); // 非 vanilla 气候布局的列：逐格回退
        }
        return col[ly];
    }

    @SuppressWarnings("unchecked")
    private Holder<Biome>[] buildColumn(int qx, int qz) {
        // 列底采样 5 个 Y 不变坐标；列顶复采校验（datapack 守护）
        Climate.TargetPoint lo = sampler.sample(qx, minQuartY, qz);
        Climate.TargetPoint hi = sampler.sample(qx, minQuartY + countY - 1, qz);
        if (lo.temperature() != hi.temperature() || lo.humidity() != hi.humidity()
                || lo.continentalness() != hi.continentalness() || lo.erosion() != hi.erosion()
                || lo.weirdness() != hi.weirdness()) {
            return FALLBACK;
        }
        long[] fixed5 = {lo.temperature(), lo.humidity(), lo.continentalness(), lo.erosion(), lo.weirdness()};
        long[] c = BiomeColumnSolver.baseFitness(boxes, offsets, fixed5);

        long[] depthPerY = new long[countY];
        for (int y = 0; y < countY; y++) {
            ctx.set(QuartPos.toBlock(qx), QuartPos.toBlock(minQuartY + y), QuartPos.toBlock(qz));
            depthPerY[y] = Climate.quantizeCoord((float) depthFn.compute(ctx));
        }
        int[] winners = BiomeColumnSolver.solveColumn(c, depthMin, depthMax, depthPerY);

        Holder<Biome>[] col = new Holder[countY];
        for (int y = 0; y < countY; y++) col[y] = biomes[winners[y]];
        return col;
    }

    /** 可变 FunctionContext，替代每次 new SinglePointContext（vanilla compute 只读坐标）。 */
    static final class MutableContext implements DensityFunction.FunctionContext {
        private int x, y, z;
        void set(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
        @Override public int blockX() { return x; }
        @Override public int blockY() { return y; }
        @Override public int blockZ() { return z; }
    }
}

package com.teammoeg.frostedheart.mixin.minecraft.misc;

import java.util.List;

import com.teammoeg.frostedheart.mixin.minecraft.accessors.CacheAllInCellAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 方案1 落地：cell 均匀密度快路径（vanilla 与 FastNoise 双环境通用）。
 *
 * 原理：NoiseChunk 的 CacheAllInCell 在 selectCellYZ 时已把本 cell 全部 128 个
 * 逐方块 (finalDensity+beardifier) 算好。若其最小值 > 0.5，则本 cell 每个方块
 * 在 vanilla 语义下必然走"含水层 0.5 早退 → 默认方块"，且规则列表首个非空即返回，
 * 矿脉根本不会被执行 ⇒ getInterpolatedState 逐位等价于直接返回 defaultBlock。
 *
 * 兼容性：
 *  - vanilla：NoiseBasedChunkGenerator#doFill 内环调用 getInterpolatedState；
 *  - FastNoise：FastWorldgen.populateNoise 内环调用 sampleBlockState()——
 *    与 getInterpolatedState 是同一方法（Yarn/Mojmap 名差异），本注入同时生效；
 *  - cellCaches.size() != 1（datapack 加了额外 cache_all_in_cell）时自动禁用。
 *
 * 正确性依赖的原版语义（已核实）：density > 0.5 → 默认方块（含水层早退，非空返回）；
 * MaterialRuleList 首个非空即返回。验证：固定 seed 区块 NBT diff。
 */
@Mixin(NoiseChunk.class)
public abstract class NoiseChunkFastPathMixin {

    @Accessor("cellCaches")
    protected abstract List<Object> fast$cellCaches();

    @Accessor("fillingCell")
    protected abstract boolean fast$fillingCell();

    @Accessor("arrayInterpolationCounter")
    protected abstract long fast$arrayInterpolationCounter();

    @Unique
    private BlockState fast$defaultBlock;
    @Unique
    private long fast$classifiedCounter = Long.MIN_VALUE;
    @Unique
    private boolean fast$cellSolid;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void fast$captureSettings(int cellCountXZ, RandomState random, int firstNoiseX, int firstNoiseZ,
                                      NoiseSettings noiseSettings, DensityFunctions.BeardifierOrMarker beardifier,
                                      NoiseGeneratorSettings generatorSettings, Aquifer.FluidPicker fluidPicker,
                                      Blender blender, CallbackInfo ci) {
        this.fast$defaultBlock = generatorSettings.defaultBlock();
    }

    @Inject(method = "getInterpolatedState", at = @At("HEAD"), cancellable = true)
    private void fast$uniformCellSkip(CallbackInfoReturnable<BlockState> cir) {
        if (fast$defaultBlock == null || !fast$fillingCell()) return;
        long counter = fast$arrayInterpolationCounter();
        if (counter != fast$classifiedCounter) {
            fast$classifiedCounter = counter;
            fast$cellSolid = fast$classifyCurrentCell();
        }
        if (fast$cellSolid) {
            cir.setReturnValue(fast$defaultBlock);
        }
    }

    /** 当前 cell 密度下界 > 0.5 ⇒ 整 cell 必为默认方块。 */
    @Unique
    private boolean fast$classifyCurrentCell() {
        List<Object> caches = fast$cellCaches();
        if (caches.size() != 1) return false; // datapack 多 cache：无法对应 finalDensity，禁用
        double[] values = ((CacheAllInCellAccessor) caches.get(0)).fast$values();
        for (double v : values) {
            if (v <= 0.5D) return false;
        }
        return true;
    }
}

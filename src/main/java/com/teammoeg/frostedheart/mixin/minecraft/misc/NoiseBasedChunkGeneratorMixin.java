package com.teammoeg.frostedheart.mixin.minecraft.misc;

import com.teammoeg.frostedheart.util.ColumnCachingBiomeResolver;
import com.teammoeg.frostedheart.util.FastNoiseEngine;
import net.minecraft.core.Holder;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {


    @Redirect(
            method = "doCreateBiomes",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkAccess;fillBiomesFromNoise(Lnet/minecraft/world/level/biome/BiomeResolver;Lnet/minecraft/world/level/biome/Climate$Sampler;)V"
            ), require = 1)
    private void fastCache$redirectFillBiomes(
            ChunkAccess chunk,
            BiomeResolver resolver,
            Climate.Sampler sampler,
            Blender blender, RandomState random, StructureManager structures, ChunkAccess chunk2
    ) {

        //群系垂直列表优化
        if (resolver instanceof MultiNoiseBiomeSource multi) {
            resolver = new ColumnCachingBiomeResolver(multi, sampler, chunk);
        }

        //使用 FastNoiseEngine 快速群系填充
        FastNoiseEngine.populateBiomes(chunk, resolver, sampler);
    }


    /**
     * 将 doFill 的调用重定向到 FastNoiseEngine 的快速噪声填充。
     */
    /**
     * 在 doFill 方法头部拦截，若满足快速路径条件，则用 FastNoiseEngine 填充并立即返回，
     * 否则原方法体正常执行。
     */
    @Inject(
            method = "doFill",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void fastNoise$tryPopulateNoise(
            Blender pBlender, StructureManager pStructureManager, RandomState pRandom, ChunkAccess pChunk, int pMinCellY, int pCellCountY, CallbackInfoReturnable<ChunkAccess> cir
    ) {
        // 获取默认方块
        Holder<NoiseGeneratorSettings> settings = ((Accessor) this).getSettings();
        BlockState defaultBlock = settings.value().defaultBlock();

        // 快速路径条件
        if (defaultBlock == FastNoiseEngine.AIR
                || pChunk.isUpgrading()
                || !FastNoiseEngine.isChunkEmpty(pChunk)) {
            return; // 回退原版
        }

        // 创建 NoiseChunk
        NoiseChunk noiseChunk = pChunk.getOrCreateNoiseChunk(
                c -> ((Accessor) this).invokeCreateNoiseChunk(c, pStructureManager, pBlender, pRandom)
        );

        // 执行快速填充
        FastNoiseEngine.populateNoise(
                noiseChunk, defaultBlock, pChunk, pMinCellY, pCellCountY,
                pChunk.getMinBuildHeight()
        );

        // 直接返回 chunk，阻止原版 doFill 执行
        cir.setReturnValue(pChunk);
    }


    @Mixin(NoiseBasedChunkGenerator.class)
    public interface Accessor {
        @org.spongepowered.asm.mixin.gen.Accessor("settings")
        Holder<NoiseGeneratorSettings> getSettings();

        @Invoker("createNoiseChunk")
        NoiseChunk invokeCreateNoiseChunk(
                ChunkAccess chunk,
                StructureManager structures,
                Blender blender,
                RandomState random
        );
    }
}
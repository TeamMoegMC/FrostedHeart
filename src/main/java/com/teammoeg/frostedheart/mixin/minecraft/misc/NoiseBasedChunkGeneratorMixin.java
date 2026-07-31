package com.teammoeg.frostedheart.mixin.minecraft.misc;

import com.teammoeg.frostedheart.util.ColumnCachingBiomeResolver;
import com.teammoeg.frostedheart.util.FastNoiseEngine;
import net.minecraft.core.Holder;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;

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


    /** 原版 doFill 方法，当不满足快速路径时回退使用 */
    @Invoker("doFill")
    abstract ChunkAccess invokeDoFill(
            Blender blender, StructureManager structures, RandomState random,
            ChunkAccess chunk, int minCellY, int cellHeight);

    @Invoker("createNoiseChunk")
    abstract NoiseChunk invokeCreateNoiseChunk(
            ChunkAccess chunk, StructureManager structures, Blender blender, RandomState random);

    /**
     * 取代 lambda$fillFromNoise$11 内部对 doFill 的调用，
     * 在满足条件时改用 FastNoiseEngine 快速填充。
     */
    @Redirect(
            method = "lambda$fillFromNoise$11",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/NoiseBasedChunkGenerator;doFill(Lnet/minecraft/world/level/levelgen/blending/Blender;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;II)Lnet/minecraft/world/level/chunk/ChunkAccess;"
            ),
            require = 1
    )
    private ChunkAccess fastNoise$redirectDoFill(
            NoiseBasedChunkGenerator self,
            Blender blender, StructureManager structures, RandomState random,
            ChunkAccess chunk, int minCellY, int cellHeight
    ) {
        // 获取默认方块
        Holder<NoiseGeneratorSettings> settings = self.generatorSettings();
        BlockState defaultBlock = settings.value().defaultBlock();

        // 快速路径条件：非空气默认方块、无 retrogen、区块当前全为空气
        if (defaultBlock != Blocks.AIR.defaultBlockState()
                && !chunk.isUpgrading()
                && FastNoiseEngine.isChunkEmpty(chunk)) {

            NoiseChunk noiseChunk = chunk.getOrCreateNoiseChunk(
                    c -> this.invokeCreateNoiseChunk(c, structures, blender, random)
            );

            FastNoiseEngine.populateNoise(
                    noiseChunk, defaultBlock, chunk,
                    minCellY, cellHeight,
                    chunk.getMinBuildHeight()
            );
            return chunk;
        }

        // 回退到原版 doFill
        return this.invokeDoFill(blender, structures, random, chunk, minCellY, cellHeight);
    }
}
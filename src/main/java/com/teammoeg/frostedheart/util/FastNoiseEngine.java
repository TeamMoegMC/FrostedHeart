/*
 * Copyright (c) 2023-2024 Zenxarch
 * Licensed under the Mozilla Public License 2.0 (MPL-2.0)
 * Modified for Frosted Heart integration.
 */
package com.teammoeg.frostedheart.util;

import com.teammoeg.frostedheart.content.climate.thermal.phase0.mutation.Phase0aMutationProbe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ZeroBitStorage;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;

import java.util.EnumSet;

/**
 * 合并 FastNoise 核心功能：快速噪声填充、快速群系填充
 *
 */
public final class FastNoiseEngine {
    @SuppressWarnings({"unchecked"})
    private static final PalettedContainer.Configuration<Holder<Biome>>[] BIOME_CONFIGS = new PalettedContainer.Configuration[] {
            new PalettedContainer.Configuration<>(PalettedContainer.Strategy.SINGLE_VALUE_PALETTE_FACTORY, 0),
            new PalettedContainer.Configuration<>(PalettedContainer.Strategy.LINEAR_PALETTE_FACTORY, 1),
            new PalettedContainer.Configuration<>(PalettedContainer.Strategy.LINEAR_PALETTE_FACTORY, 2),
            new PalettedContainer.Configuration<>(PalettedContainer.Strategy.LINEAR_PALETTE_FACTORY, 3),
            new PalettedContainer.Configuration<>(PalettedContainer.Strategy.LINEAR_PALETTE_FACTORY, 4),
            new PalettedContainer.Configuration<>(PalettedContainer.Strategy.LINEAR_PALETTE_FACTORY, 5),
            new PalettedContainer.Configuration<>(PalettedContainer.Strategy.LINEAR_PALETTE_FACTORY, 6)
    };
    // ---- 噪声填充缓存 ----
    private static final PalettedContainer.Configuration<BlockState> BLOCK_CONFIG =
            new PalettedContainer.Configuration<>(PalettedContainer.Strategy.LINEAR_PALETTE_FACTORY, 4);

    public static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private FastNoiseEngine() {}

    // ---- 快速噪声填充 ----
    public static void populateNoise(
            NoiseChunk noiseChunk,
            BlockState defaultBlock,
            ChunkAccess chunk,
            int minCellY, int cellHeight,
            int minBuildHeight
    ) {
        ChunkPos pos = chunk.getPos();
        int chunkStartX = pos.getMinBlockX();
        int chunkStartZ = pos.getMinBlockZ();
        Aquifer aquifer = noiseChunk.aquifer();
        noiseChunk.initializeForFirstCellX();
        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();

        int cellW = noiseChunk.cellWidth();
        int cellH = noiseChunk.cellHeight();
        int cellsXZ = 16 / cellW;

        FastSectionData[] fastSections = new FastSectionData[chunk.getSections().length];

        for (int cx = 0; cx < cellsXZ; cx++) {
            noiseChunk.advanceCellX(cx);
            for (int cz = 0; cz < cellsXZ; cz++) {
                int secIndex = -1;
                LevelChunkSection section = null;
                for (int cy = cellHeight - 1; cy >= 0; cy--) {
                    noiseChunk.selectCellYZ(cy, cz);
                    for (int by = cellH - 1; by >= 0; by--) {
                        int worldY = (minCellY + cy) * cellH + by;
                        int secY = worldY & 15;
                        int secIdx = chunk.getSectionIndex(worldY);
                        if (secIndex != secIdx) {
                            secIndex = secIdx;
                            section = chunk.getSection(secIdx);
                        }
                        double yProgress = (double) by / cellH;
                        noiseChunk.updateForY(worldY, yProgress);

                        for (int bx = 0; bx < cellW; bx++) {
                            int worldX = chunkStartX + cx * cellW + bx;
                            int secX = worldX & 15;
                            double xProgress = (double) bx / cellW;
                            noiseChunk.updateForX(worldX, xProgress);

                            for (int bz = 0; bz < cellW; bz++) {
                                int worldZ = chunkStartZ + cz * cellW + bz;
                                int secZ = worldZ & 15;
                                double zProgress = (double) bz / cellW;
                                noiseChunk.updateForZ(worldZ, zProgress);

                                BlockState state = noiseChunk.getInterpolatedState();
                                if (state == null) state = defaultBlock;
                                if (state == AIR) continue;

                                FastSectionData fsd = fastSections[secIdx];
                                if (fsd == null) {
                                    fsd = new FastSectionData(section);
                                    fsd.setDefaultBlock(defaultBlock);
                                    fastSections[secIdx] = fsd;
                                }
                                fsd.setBlockState(section, secX, secY, secZ, state);

                                if (aquifer.shouldScheduleFluidUpdate() && !state.getFluidState().isEmpty()) {
                                    mPos.set(worldX, worldY, worldZ);
                                    chunk.markPosForPostprocessing(mPos);
                                }
                            }
                        }
                    }
                }
            }
            noiseChunk.swapSlices();
        }
        noiseChunk.stopInterpolation();

        // 结束填充，重算计数
        for (int i = 0; i < fastSections.length; i++) {
            if (fastSections[i] != null) {
                LevelChunkSection section = chunk.getSection(i);
                fastSections[i].finish(section);
                Phase0aMutationProbe.onRawBlockContainerReplaced(section);
            }
        }

        Heightmap.primeHeightmaps(chunk, EnumSet.of(Heightmap.Types.OCEAN_FLOOR_WG, Heightmap.Types.WORLD_SURFACE_WG));
    }

    public static boolean isChunkEmpty(ChunkAccess chunk) {
        for (LevelChunkSection section : chunk.getSections()) {
            if (!section.hasOnlyAir()) return false;
        }
        return true;
    }

    // ---- 快速群系填充 ----
    @SuppressWarnings("unchecked")
    public static void populateBiomes(
            ChunkAccess chunk,
            BiomeResolver resolver,
            Climate.Sampler sampler
    ) {
        int minQX = QuartPos.fromBlock(chunk.getPos().getMinBlockX());
        int minQZ = QuartPos.fromBlock(chunk.getPos().getMinBlockZ());
        int minQY = QuartPos.fromBlock(chunk.getMinBuildHeight());
        int maxQY = minQY + chunk.getSections().length * 4;

        Holder<Biome>[] biomes = new Holder[64];
        byte[] storage = new byte[64];

        for (int sy = 0; sy < chunk.getSections().length; sy++) {
            int sectionQY = minQY + sy * 4;
            if (sectionQY >= maxQY) break;
            LevelChunkSection section = chunk.getSection(sy);

            int size = 0;
            for (int y = 0; y < 4; y++) {
                int qy = sectionQY + y;
                for (int z = 0; z < 4; z++) {
                    int qz = minQZ + z;
                    for (int x = 0; x < 4; x++) {
                        int qx = minQX + x;
                        Holder<Biome> biome = resolver.getNoiseBiome(qx, qy, qz, sampler);
                        int idx = -1;
                        for (int i = 0; i < size; i++) {
                            if (biomes[i] == biome) { idx = i; break; }
                        }
                        if (idx == -1) {
                            idx = size++;
                            biomes[idx] = biome;
                        }
                        storage[(y << 4) | (z << 2) | x] = (byte) idx;
                    }
                }
            }

            PalettedContainer<Holder<Biome>> container = (PalettedContainer<Holder<Biome>>) section.biomes;
            if (size == 1) {
                if (container.data.palette() instanceof SingleValuePalette<Holder<Biome>> existing) {
                    existing.value = biomes[0];
                } else {
                    container.data = new PalettedContainer.Data<>(
                            BIOME_CONFIGS[0 ],
                            new ZeroBitStorage(64),
                            new SingleValuePalette<>(container.registry, container, java.util.List.of(biomes[0]))
                    );
                }
            } else {
                int bits = Mth.ceillog2(size);
                Holder<Biome>[] palette = new Holder[1 << bits];
                System.arraycopy(biomes, 0, palette, 0, size);
                long[] packed = packStorage(storage, bits);
                container.data = new PalettedContainer.Data<>(
                        BIOME_CONFIGS[bits],
                        new SimpleBitStorage(bits, 64, packed),
                        new LinearPalette<>(container.registry, palette, container, bits, size)
                );
            }
            Phase0aMutationProbe.onRawBiomeContainerReplaced(section);
        }
    }

    private static long[] packStorage(byte[] data, int bits) {
        int valuesPerLong = 64 / bits;
        int longCount = (data.length + valuesPerLong - 1) / valuesPerLong;
        long[] result = new long[longCount];
        for (int i = 0; i < data.length; i++) {
            int longIdx = i / valuesPerLong;
            int bitOffset = (i % valuesPerLong) * bits;
            result[longIdx] |= ((long) data[i] & ((1L << bits) - 1)) << bitOffset;
        }
        return result;
    }

    private static class FastSectionData {
        final BlockState[] states = new BlockState[16];
        final long[] storage;
        int size = 1;           // index 0 = air
        int defaultIdx = -1;    // cached index for default block
        int minIdx = 1;
        final LinearPalette<BlockState> palette;

        @SuppressWarnings("unchecked")
        FastSectionData(LevelChunkSection section) {
            states[0] = Blocks.AIR.defaultBlockState();
            storage = new long[256];
            this.palette = new LinearPalette<>(
                    section.states.registry,
                    states,
                    section.states,
                    4,
                    size
            );
            section.states.data = new PalettedContainer.Data<>(
                    BLOCK_CONFIG,
                    new SimpleBitStorage(4, 4096, this.storage),
                    palette
            );
        }

        void setDefaultBlock(BlockState defaultBlock) {
            if (defaultIdx != -1) return;
            for (int i = 0; i < size; i++) {
                if (states[i] == defaultBlock) {
                    defaultIdx = i;
                    return;
                }
            }
            if (size >= 16) return;
            defaultIdx = size;
            states[size] = defaultBlock;
            size++;
            palette.size = size;
        }

        void setBlockState(LevelChunkSection section, int x, int y, int z, BlockState state) {
            int idx;
            if (defaultIdx != -1 && state == states[defaultIdx]) {
                idx = defaultIdx;
            } else {
                idx = findIndex(state);
                if (idx == -1) {
                    if (size >= 16) return;
                    idx = size;
                    states[size] = state;
                    size++;
                    palette.size = size;
                }
            }
            storage[(y << 4) | z] |= (long) idx << (x << 2);
        }

        private int findIndex(BlockState state) {
            for (int i = minIdx; i < size; i++) {
                if (states[i] == state) return i;
            }
            return -1;
        }

        /** 填充完成后只重算计数，不做任何数据替换或回收 */
        void finish(LevelChunkSection section) {
            section.recalcBlockCounts();
            // storage 已经被 section 持有，不能回收
        }
    }
}

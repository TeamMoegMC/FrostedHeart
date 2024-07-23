/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.world.civilization.orbit.observatory;

import com.mojang.serialization.Codec;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public class ObservatoryStructure extends StructureFeature<NoneFeatureConfiguration> {
    public static class Start extends StructureStart<NoneFeatureConfiguration> {
        public Start(StructureFeature<NoneFeatureConfiguration> structureIn, int chunkX, int chunkZ, BoundingBox boundingBox, int referenceIn, long seedIn) {
            super(structureIn, chunkX, chunkZ, boundingBox, referenceIn, seedIn);
        }

        @Override
        public void generatePieces(RegistryAccess dynamic, ChunkGenerator generator, StructureManager template, int chunkX, int chunkZ, Biome biome, NoneFeatureConfiguration config) {

            int x = chunkX << 4;
            int z = chunkZ << 4;
            int surfaceY = generator.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG);

            BlockPos blockpos = new BlockPos(x, surfaceY, z);

            Rotation rotation = Rotation.getRandom(this.random);
            this.pieces.add(new ObservatoryPiece(template, blockpos, rotation));

            this.calculateBoundingBox();
//            FHMain.LOGGER.log(Level.DEBUG, "Observatory at " + (blockpos.getX()) + " " + blockpos.getY() + " " + (blockpos.getZ()));
        }
    }


    public ObservatoryStructure(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected boolean isFeatureChunk(ChunkGenerator generator, BiomeSource biomeprovider, long seed, WorldgenRandom random, int chunkX, int chunkZ, Biome biome, ChunkPos chunkPos, NoneFeatureConfiguration p_230363_10_) {
        BlockPos centerOfChunk = new BlockPos(chunkX * 16, 0, chunkZ * 16);

        int landHeight = generator.getFirstFreeHeight(centerOfChunk.getX(), centerOfChunk.getZ(), Heightmap.Types.WORLD_SURFACE_WG);
        if (landHeight < 100 || landHeight > 200) return false;
        BlockGetter columnOfBlocks = generator.getBaseColumn(centerOfChunk.getX(), centerOfChunk.getZ());

        BlockState topBlock = columnOfBlocks.getBlockState(centerOfChunk.above(landHeight));

        return topBlock.getFluidState().isEmpty();

    }


    @Override
    public GenerationStep.Decoration step() {
        return GenerationStep.Decoration.SURFACE_STRUCTURES;
    }

    public StructureFeature.StructureStartFactory<NoneFeatureConfiguration> getStartFactory() {
        return ObservatoryStructure.Start::new;
    }
}

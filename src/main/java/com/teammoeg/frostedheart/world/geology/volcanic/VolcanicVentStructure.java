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

package com.teammoeg.frostedheart.world.geology.volcanic;

import com.mojang.serialization.Codec;

import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.template.TemplateManager;

public class VolcanicVentStructure extends Structure<NoFeatureConfig> {
    public static class Start extends StructureStart<NoFeatureConfig> {
        public Start(Structure<NoFeatureConfig> p_i225819_1_, int p_i225819_2_, int p_i225819_3_, MutableBoundingBox boundingBox, int p_i225819_5_, long p_i225819_6_) {
            super(p_i225819_1_, p_i225819_2_, p_i225819_3_, boundingBox, p_i225819_5_, p_i225819_6_);
        }

        @Override
        public void func_230364_a_(DynamicRegistries dynamic, ChunkGenerator generator, TemplateManager template, int chunkX, int chunkZ, Biome biome, NoFeatureConfig config) {

            int x = chunkX << 4;
            int z = chunkZ << 4;
            int surfaceY = generator.getNoiseHeightMinusOne(x, z, Heightmap.Type.WORLD_SURFACE_WG);
            BlockPos blockpos = new BlockPos(x, surfaceY, z);

            this.components.add(new VolcanicVentPiece(blockpos));
            this.recalculateStructureSize();
//            FHMain.LOGGER.debug( "volcanic at " + (blockpos.getX()) + " " + blockpos.getY() + " " + (blockpos.getZ()));
        }
    }


    public VolcanicVentStructure(Codec<NoFeatureConfig> codec) {
        super(codec);
    }

    @Override
    protected boolean func_230363_a_(ChunkGenerator generator, BiomeProvider biomeprovider, long p_230363_3_, SharedSeedRandom seed, int chunkX, int chunkZ, Biome biome, ChunkPos chunkPos, NoFeatureConfig p_230363_10_) {
        return true;
    }


    @Override
    public GenerationStage.Decoration getDecorationStage() {
        return GenerationStage.Decoration.SURFACE_STRUCTURES;
    }

    public Structure.IStartFactory<NoFeatureConfig> getStartFactory() {
        return Start::new;
    }
}

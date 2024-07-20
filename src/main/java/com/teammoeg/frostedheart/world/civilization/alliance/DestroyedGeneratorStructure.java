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

package com.teammoeg.frostedheart.world.civilization.alliance;


import net.minecraft.block.BlockState;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedSeedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.GenerationStage;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.jigsaw.JigsawManager;
import net.minecraft.world.gen.feature.structure.AbstractVillagePiece;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraft.world.gen.feature.structure.VillageConfig;
import net.minecraft.world.gen.feature.template.TemplateManager;
import com.teammoeg.frostedheart.FHMain;

public class DestroyedGeneratorStructure extends Structure<NoFeatureConfig> {

    public DestroyedGeneratorStructure() {
        super(NoFeatureConfig.CODEC);
    }

    @Override
    public GenerationStage.Decoration getDecorationStage() {
        return GenerationStage.Decoration.SURFACE_STRUCTURES;
    }

    @Override
    public IStartFactory<NoFeatureConfig> getStartFactory() {
        return DestroyedGeneratorStructure.Start::new;
    }

    /*private static final List<MobSpawnInfo.Spawners> STRUCTURE_MONSTERS = ImmutableList.of(
            new MobSpawnInfo.Spawners(EntityType.ZOMBIE, 100, 4, 9)
    );

    @Override
    public List<MobSpawnInfo.Spawners> getDefaultSpawnList() {
        return STRUCTURE_MONSTERS;
    }*/

    public static class Start extends StructureStart<NoFeatureConfig> {
        public Start(Structure<NoFeatureConfig> structureIn, int chunkX, int chunkZ,
                     MutableBoundingBox mutableBoundingBox, int referenceIn, long seedIn) {
            super(structureIn, chunkX, chunkZ, mutableBoundingBox, referenceIn, seedIn);
        }

        @Override //GeneratePieces
        public void func_230364_a_(DynamicRegistries dynamicRegistryManager, ChunkGenerator chunkGenerator,
                                   TemplateManager templateManagerIn, int chunkX, int chunkZ, Biome biome,
                                   NoFeatureConfig config) {

            // Turns the chunk coordinates into actual coordinates we can use. (Gets center of that chunk)
            int x = (chunkX << 4) + 7;
            int z = (chunkZ << 4) + 7;

            BlockPos centerOfChunk = new BlockPos(x, 0, z);
            int y = chunkGenerator.getHeight(centerOfChunk.getX(), centerOfChunk.getZ(), Heightmap.Type.WORLD_SURFACE_WG);

            BlockPos blockpos = new BlockPos(x, y, z);

            //addpieces()
            JigsawManager.func_242837_a(dynamicRegistryManager,
                    new VillageConfig(() -> dynamicRegistryManager.getRegistry(Registry.JIGSAW_POOL_KEY)
                            .getOrDefault(new ResourceLocation(FHMain.MODID, "destroyed_generator/start_pool")),
                            5), //Pool size
                    AbstractVillagePiece::new,
                    chunkGenerator,
                    templateManagerIn,
                    blockpos,
                    this.components,
                    this.rand,
                    false,
                    false);

            this.recalculateStructureSize();
//            FHMain.LOGGER.log(Level.DEBUG, "Observatory at " + (blockpos.getX()) + " " + blockpos.getY() + " " + (blockpos.getZ()));
        }
    }

    @Override
    protected boolean func_230363_a_(ChunkGenerator chunkGenerator, BiomeProvider biomeSource,
                                     long seed, SharedSeedRandom chunkRandom, int chunkX, int chunkZ,
                                     Biome biome, ChunkPos chunkPos, NoFeatureConfig featureConfig) {

        BlockPos centerOfChunk = new BlockPos((chunkX << 4) + 7, 0, (chunkZ << 4) + 7);

        int landHeight = chunkGenerator.getHeight(centerOfChunk.getX(), centerOfChunk.getZ(), Heightmap.Type.WORLD_SURFACE_WG);
        IBlockReader columnOfBlocks = chunkGenerator.func_230348_a_(centerOfChunk.getX(), centerOfChunk.getZ());
        BlockState topBlock = columnOfBlocks.getBlockState(centerOfChunk.up(landHeight));

        return topBlock.getFluidState().isEmpty(); //This Func verifies if fluid exists on the area..
    }

}

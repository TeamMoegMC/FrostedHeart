package com.teammoeg.frostedheart.world.structure;

import com.mojang.serialization.Codec;
import com.teammoeg.frostedheart.FHMain;
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
import org.apache.logging.log4j.Level;

public class VolcanicVentStructure extends Structure<NoFeatureConfig>  {
    public VolcanicVentStructure(Codec<NoFeatureConfig> codec) {
        super(codec);
    }


    public Structure.IStartFactory<NoFeatureConfig> getStartFactory() {
        return Start::new;
    }
    @Override
    public GenerationStage.Decoration getDecorationStage() {
        return GenerationStage.Decoration.SURFACE_STRUCTURES;
    }


    @Override
    protected boolean func_230363_a_(ChunkGenerator generator, BiomeProvider biomeprovider, long p_230363_3_, SharedSeedRandom seed, int chunkX, int chunkZ, Biome biome, ChunkPos chunkPos, NoFeatureConfig p_230363_10_) {
        return true;
    }

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
}

package com.teammoeg.frostedheart.mixin.rankine;

import com.cannolicatfish.rankine.init.RankineLists;
import com.cannolicatfish.rankine.util.WorldgenUtils;
import com.cannolicatfish.rankine.world.gen.feature.PostWorldReplacerFeature;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SnowyDirtBlock;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Random;

@Mixin(PostWorldReplacerFeature.class)
public class MixinPostWorldReplacer {
    /**
     * @author dasb
     * @reason
     */
    @Overwrite(remap = false)
    public boolean generate(ISeedReader reader, ChunkGenerator generator, Random rand, BlockPos pos, NoFeatureConfig config) {
        IChunk chunk = reader.getChunk(pos);
        for (int x = chunk.getPos().getXStart(); x <= chunk.getPos().getXEnd(); ++x) {
            for (int z = chunk.getPos().getZStart(); z <= chunk.getPos().getZEnd(); ++z) {
                int endY = reader.getHeight(Heightmap.Type.OCEAN_FLOOR_WG, x, z);

                for (int y = 52; y < endY; ++y) {
                    BlockPos TARGET_POS = new BlockPos(x, y, z);
                    Block TARGET = reader.getBlockState(TARGET_POS).getBlock();
                    ResourceLocation TARGET_BIOME = reader.getBiome(TARGET_POS).getRegistryName();
                    if (WorldgenUtils.GEN_BIOMES.contains(TARGET_BIOME)) {
                        int genBiomesIndex = WorldgenUtils.GEN_BIOMES.indexOf(TARGET_BIOME);

                        if (TARGET.matchesBlock(Blocks.GRASS_BLOCK)) {
                            Block Olayer = WorldgenUtils.O1.get(genBiomesIndex);
                            if (Olayer instanceof SnowyDirtBlock) {
                                if (reader.getBlockState(TARGET_POS).get(BlockStateProperties.SNOWY)) {
                                    reader.setBlockState(TARGET_POS, Olayer.getDefaultState().with(BlockStateProperties.SNOWY, true), 2);
                                } else if (RankineLists.GRASS_BLOCKS.contains(Olayer) && WorldgenUtils.isWet(reader, TARGET_POS)) {
                                    reader.setBlockState(TARGET_POS, RankineLists.MUD_BLOCKS.get(RankineLists.GRASS_BLOCKS.indexOf(Olayer)).getDefaultState(), 2);
                                } else {
                                    reader.setBlockState(TARGET_POS, Olayer.getDefaultState(), 2);
                                }
                            } else {
                                reader.setBlockState(TARGET_POS, Olayer.getDefaultState(), 2);
                            }
                        } else if (TARGET.matchesBlock(Blocks.DIRT)) {
                            Block Alayer = WorldgenUtils.A1.get(genBiomesIndex);
                            if (RankineLists.SOIL_BLOCKS.contains(Alayer) && WorldgenUtils.isWet(reader, TARGET_POS)) {
                                reader.setBlockState(TARGET_POS, RankineLists.MUD_BLOCKS.get(RankineLists.SOIL_BLOCKS.indexOf(Alayer)).getDefaultState(), 2);
                            } else {
                                reader.setBlockState(TARGET_POS, Alayer.getDefaultState(), 2);
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}

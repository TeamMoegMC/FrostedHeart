package com.teammoeg.frostedheart.content.world.features;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

/** Shared, bounded terrain predicates for the ice-cave decoration pass. */
final class IceCaveTerrain {
    static final int MIN_Y = -63;
    static final int MAX_Y = 128;
    static final ResourceLocation BIOME = new ResourceLocation("the_winter_rescue", "ice_caves");

    private IceCaveTerrain() {}

    static boolean inBiome(WorldGenLevel level, BlockPos pos) {
        return level.getBiome(pos).is(BIOME);
    }

    static boolean ice(BlockState state) {
        return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE)
                || state.is(FHBlocks.FIRM_ICE_BLOCK.get());
    }

    static boolean stone(BlockState state) {
        return !state.is(Tags.Blocks.ORES) && (state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.STONE_ORE_REPLACEABLES) || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || state.is(Blocks.CALCITE) || state.is(Blocks.DIRT) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY));
    }

    static boolean substrate(BlockState state) {
        return stone(state) || ice(state);
    }

    static boolean springNearby(WorldGenLevel level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (level.getFluidState(pos.offset(dx, dy, dz)).getType().isSame(FHFluids.HOT_SPRING.get())) return true;
            }
        }
        return false;
    }
}

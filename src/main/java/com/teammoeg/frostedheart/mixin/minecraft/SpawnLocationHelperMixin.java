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

package com.teammoeg.frostedheart.mixin.minecraft;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.server.level.ServerLevel;
/**
 * Make spawnpoint more suitable
 * For removal in 1.20+
 * */
@Mixin(PlayerRespawnLogic.class)
public class SpawnLocationHelperMixin {
    /**
     * @author khjxiaogu
     * @reason To make spawn point more suitable
     */
    @Overwrite
    @Nullable
    public static BlockPos getOverworldRespawnPos(ServerLevel p_241092_0_, int p_241092_1_, int p_241092_2_,
                                          boolean p_241092_3_) {
        BlockPos.MutableBlockPos blockpos$mutable = new BlockPos.MutableBlockPos(p_241092_1_, 0, p_241092_2_);
        Biome biome = p_241092_0_.getBiome(blockpos$mutable);
        boolean flag = p_241092_0_.dimensionType().hasCeiling();
        BlockState blockstate = biome.getGenerationSettings().getSurfaceBuilderConfig().getTopMaterial();
        if (p_241092_3_ && !blockstate.getBlock().is(BlockTags.VALID_SPAWN)) {
            return null;
        }
        LevelChunk chunk = p_241092_0_.getChunk(p_241092_1_ >> 4, p_241092_2_ >> 4);
        int i = flag ? p_241092_0_.getChunkSource().getGenerator().getSpawnHeight()
                : chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, p_241092_1_ & 15, p_241092_2_ & 15);
        if (i < 0) {
            return null;
        }
        int j = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, p_241092_1_ & 15, p_241092_2_ & 15);
        if (j <= i && j > chunk.getHeight(Heightmap.Types.OCEAN_FLOOR, p_241092_1_ & 15, p_241092_2_ & 15)) {
            return null;
        }
        for (int k = i + 3; k >= 0; --k) {
            blockpos$mutable.set(p_241092_1_, k, p_241092_2_);
            BlockState blockstate1 = p_241092_0_.getBlockState(blockpos$mutable);
            if (!blockstate1.getFluidState().isEmpty()) {
                break;
            }

            if (blockstate1.equals(blockstate) || blockstate1.getBlock().is(BlockTags.VALID_SPAWN)) {
                return blockpos$mutable.above().immutable();
            }
        }

        return null;
    }

}

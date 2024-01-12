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

package com.teammoeg.frostedheart.mixin.create;

import com.simibubi.create.AllFluids;
import com.simibubi.create.content.contraptions.fluids.FluidReactions;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FluidReactions.class)
public class FluidReactionsMixin {
    /**
     * @author khjxiaogu
     * @reason make create call forge events
     */
    @Overwrite(remap = false)
    public static void handlePipeSpillCollision(World world, BlockPos pos, Fluid pipeFluid, FluidState worldFluid) {
        Fluid pf = FluidHelper.convertToStill(pipeFluid);
        Fluid wf = worldFluid.getFluid();
        if (pf.isIn(FluidTags.WATER) && wf == Fluids.LAVA)
            world.setBlockState(pos, ForgeEventFactory.fireFluidPlaceBlockEvent(world, pos, pos, Blocks.OBSIDIAN.getDefaultState()));
        else if (pf == Fluids.WATER && wf == Fluids.FLOWING_LAVA)
            world.setBlockState(pos, ForgeEventFactory.fireFluidPlaceBlockEvent(world, pos, pos, Blocks.COBBLESTONE.getDefaultState()));
        else if (pf == Fluids.LAVA && wf == Fluids.WATER)
            world.setBlockState(pos, ForgeEventFactory.fireFluidPlaceBlockEvent(world, pos, pos, Blocks.STONE.getDefaultState()));
        else if (pf == Fluids.LAVA && wf == Fluids.FLOWING_WATER)
            world.setBlockState(pos, ForgeEventFactory.fireFluidPlaceBlockEvent(world, pos, pos, Blocks.COBBLESTONE.getDefaultState()));

        if (pf == Fluids.LAVA) {
            BlockState lavaInteraction = AllFluids.getLavaInteraction(worldFluid);
            if (lavaInteraction != null)
                world.setBlockState(pos, lavaInteraction);
        } else if (wf == Fluids.FLOWING_LAVA && FluidHelper.hasBlockState(pf)) {
            BlockState lavaInteraction = AllFluids.getLavaInteraction(FluidHelper.convertToFlowing(pf)
                    .getDefaultState());
            if (lavaInteraction != null)
                world.setBlockState(pos, ForgeEventFactory.fireFluidPlaceBlockEvent(world, pos, pos, lavaInteraction));
        }
    }
}

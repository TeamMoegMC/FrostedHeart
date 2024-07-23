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

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.simibubi.create.AllFluids;
import com.simibubi.create.content.contraptions.fluids.FluidReactions;
import com.simibubi.create.foundation.fluid.FluidHelper;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;

@Mixin(FluidReactions.class)
public class FluidReactionsMixin {
    /**
     * @author khjxiaogu
     * @reason make create call forge events
     */
    @Overwrite(remap = false)
    public static void handlePipeSpillCollision(Level world, BlockPos pos, Fluid pipeFluid, FluidState worldFluid) {
        Fluid pf = FluidHelper.convertToStill(pipeFluid);
        Fluid wf = worldFluid.getType();
        if (pf.is(FluidTags.WATER) && wf == Fluids.LAVA)
            world.setBlockAndUpdate(pos, ForgeEventFactory.fireFluidPlaceBlockEvent(world, pos, pos, Blocks.OBSIDIAN.defaultBlockState()));
        else if (pf == Fluids.WATER && wf == Fluids.FLOWING_LAVA)
            world.setBlockAndUpdate(pos, ForgeEventFactory.fireFluidPlaceBlockEvent(world, pos, pos, Blocks.COBBLESTONE.defaultBlockState()));
        else if (pf == Fluids.LAVA && wf == Fluids.WATER)
            world.setBlockAndUpdate(pos, ForgeEventFactory.fireFluidPlaceBlockEvent(world, pos, pos, Blocks.STONE.defaultBlockState()));
        else if (pf == Fluids.LAVA && wf == Fluids.FLOWING_WATER)
            world.setBlockAndUpdate(pos, ForgeEventFactory.fireFluidPlaceBlockEvent(world, pos, pos, Blocks.COBBLESTONE.defaultBlockState()));

        if (pf == Fluids.LAVA) {
            BlockState lavaInteraction = AllFluids.getLavaInteraction(worldFluid);
            if (lavaInteraction != null)
                world.setBlockAndUpdate(pos, lavaInteraction);
        } else if (wf == Fluids.FLOWING_LAVA && FluidHelper.hasBlockState(pf)) {
            BlockState lavaInteraction = AllFluids.getLavaInteraction(FluidHelper.convertToFlowing(pf)
                    .defaultFluidState());
            if (lavaInteraction != null)
                world.setBlockAndUpdate(pos, ForgeEventFactory.fireFluidPlaceBlockEvent(world, pos, pos, lavaInteraction));
        }
    }
}

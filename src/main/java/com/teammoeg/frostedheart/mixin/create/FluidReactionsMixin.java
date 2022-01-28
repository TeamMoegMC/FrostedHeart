package com.teammoeg.frostedheart.mixin.create;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

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
@Mixin(FluidReactions.class)
public class FluidReactionsMixin {
	/**
	 * 
	 * @author khjxiaogu
	 * @reason make create call forge events
	 * */
	@Overwrite(remap=false)
	public static void handlePipeSpillCollision(World world, BlockPos pos, Fluid pipeFluid, FluidState worldFluid) {
		Fluid pf = FluidHelper.convertToStill(pipeFluid);
		Fluid wf = worldFluid.getFluid();
		if (pf.isIn(FluidTags.WATER) && wf == Fluids.LAVA)
			world.setBlockState(pos,ForgeEventFactory.fireFluidPlaceBlockEvent(world,pos,pos,Blocks.OBSIDIAN.getDefaultState()));
		else if (pf == Fluids.WATER && wf == Fluids.FLOWING_LAVA)
			world.setBlockState(pos,ForgeEventFactory.fireFluidPlaceBlockEvent(world,pos,pos,Blocks.COBBLESTONE.getDefaultState()));
		else if (pf == Fluids.LAVA && wf == Fluids.WATER)
			world.setBlockState(pos,ForgeEventFactory.fireFluidPlaceBlockEvent(world,pos,pos,Blocks.STONE.getDefaultState()));
		else if (pf == Fluids.LAVA && wf == Fluids.FLOWING_WATER)
			world.setBlockState(pos,ForgeEventFactory.fireFluidPlaceBlockEvent(world,pos,pos,Blocks.COBBLESTONE.getDefaultState()));

		if (pf == Fluids.LAVA) {
			BlockState lavaInteraction = AllFluids.getLavaInteraction(worldFluid);
			if (lavaInteraction != null)
				world.setBlockState(pos, lavaInteraction);
		} else if (wf == Fluids.FLOWING_LAVA && FluidHelper.hasBlockState(pf)) {
			BlockState lavaInteraction = AllFluids.getLavaInteraction(FluidHelper.convertToFlowing(pf)
				.getDefaultState());
			if (lavaInteraction != null)
				world.setBlockState(pos,ForgeEventFactory.fireFluidPlaceBlockEvent(world,pos,pos,lavaInteraction));
		}
	}
}

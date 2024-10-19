package com.teammoeg.frostedheart.content.climate.heatdevice.components;

import blusunrize.immersiveengineering.api.multiblocks.blocks.util.MultiblockOrientation;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class UnregistAndRemoveHeat extends RemoveHeatAreaDisassembler {

	public UnregistAndRemoveHeat(IETemplateMultiblock mb) {
		super(mb);
	}

	@Override
	public void disassemble(Level world, BlockPos origin, MultiblockOrientation orientation) {
		super.disassemble(world, origin, orientation);
	}

}

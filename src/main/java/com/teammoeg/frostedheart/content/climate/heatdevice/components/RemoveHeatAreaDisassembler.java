package com.teammoeg.frostedheart.content.climate.heatdevice.components;

import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;

import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration.Disassembler;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.MultiblockOrientation;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class RemoveHeatAreaDisassembler implements Disassembler {
	IETemplateMultiblock mb;
	public RemoveHeatAreaDisassembler(IETemplateMultiblock mb) {
			this.mb=mb;
	}

	@Override
	public void disassemble(Level world, BlockPos origin, MultiblockOrientation orientation) {
		BlockPos master=mb.getMasterFromOriginOffset();
		 ChunkHeatData.removeTempAdjust(world, origin.offset(orientation.getAbsoluteOffset(master)));
	}

}

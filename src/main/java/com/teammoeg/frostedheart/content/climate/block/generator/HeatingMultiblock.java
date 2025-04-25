package com.teammoeg.frostedheart.content.climate.block.generator;

import com.teammoeg.chorda.multiblock.CMultiblock;
import com.teammoeg.frostedheart.content.climate.gamedata.chunkheat.ChunkHeatData;

import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public abstract class HeatingMultiblock extends CMultiblock {

	public HeatingMultiblock(ResourceLocation loc, BlockPos masterFromOrigin, BlockPos triggerFromOrigin, BlockPos size, MultiblockRegistration<?> baseState) {
		super(loc, masterFromOrigin, triggerFromOrigin, size, baseState);
		// TODO Auto-generated constructor stub
	}
    @Override
    public void disassemble(Level world, BlockPos origin, boolean mirrored, Direction clickDirectionAtCreation) {
        BlockPos master = this.getMasterFromOriginOffset();
        ChunkHeatData.removeTempAdjust(world, origin.offset(master).below(master.getY()));
        super.disassemble(world, origin, mirrored, clickDirectionAtCreation);
    }
}

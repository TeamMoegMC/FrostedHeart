package com.teammoeg.frostedheart.content.robotics.logistics.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class RequestLogisticChest extends LogisticChest {

	public RequestLogisticChest(Level level, BlockPos pos) {
		super(level, pos);
	}

	@Override
	public boolean fillable() {
		// TODO Auto-generated method stub
		return false;
	}

}

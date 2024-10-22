package com.teammoeg.frostedheart.util;

import java.util.Optional;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockBEHelper;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockLevel;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockBE;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;

public class FHMultiblockHelper {

	private FHMultiblockHelper() {
	}
	public static Optional<IMultiblockBEHelper<?>> getBEHelper(Level level,BlockPos pos){
		if(FHUtils.getExistingTileEntity(level, pos) instanceof IMultiblockBE te) {
			return Optional.of(te.getHelper());
		}
		return Optional.empty();
	}
	public static Optional<IMultiblockLogic<?>> getMultiblockLogic(Level level,BlockPos pos){
		return getBEHelper(level,pos).map(t->t.getMultiblock().logic());
	}
	public static Optional<IMultiblockBEHelper<?>> getBEHelper(IMultiblockLevel level) {
		if(level.getBlockEntity(BlockPos.ZERO) instanceof IMultiblockBE te) 
			return Optional.of(te.getHelper());
		return Optional.empty();
	}
	public static Vec3i getSize(IMultiblockLevel level) {
		return getBEHelper(level).map(t->t.getMultiblock().getSize().apply(level.getRawLevel())).orElse(Vec3i.ZERO);
	}
	public static BlockPos getMasterPos(IMultiblockLevel level) {
		return getBEHelper(level).map(t->t.getMultiblock().masterPosInMB()).orElse(BlockPos.ZERO);
	}
	public static BlockPos getAbsoluteMaster(IMultiblockLevel level) {
		return getBEHelper(level).map(t->level.toAbsolute(t.getMultiblock().masterPosInMB())).orElse(BlockPos.ZERO);
	}
}

package com.teammoeg.chorda.util.ie;

import blusunrize.immersiveengineering.api.multiblocks.MultiblockHandler.IMultiblock;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockBEHelper;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;

public interface DisassembleListener<S extends IMultiblockState> {
	void onDisassemble(IMultiblock block,IMultiblockBEHelper<S> helper);
}

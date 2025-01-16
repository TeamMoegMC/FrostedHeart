package com.teammoeg.frostedheart.mixin.immersiveengineering;

import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.AssemblerLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.teammoeg.chorda.multiblock.components.IOwnerState;
import com.teammoeg.chorda.util.mixin.IOwnerTile;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.common.blocks.metal.CrafterPatternInventory;
import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.AssemblerLogic.State;

@Mixin(AssemblerLogic.class)
public class AssemblerLogicMixin {

	public AssemblerLogicMixin() {
	}
	@Inject(at = @At("HEAD"), remap = false, method = "tickServer")
	public void fh$tickServer(IMultiblockContext<State> context,CallbackInfo cbi) {
		for(CrafterPatternInventory i:context.getState().patterns) {
			if(i instanceof IOwnerTile iot) {
				iot.setStoredOwner(((IOwnerState)context.getState()).getOwner());
			}
		}
	}
}

package com.teammoeg.frostedheart.mixin.immersiveengineering;

import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.base.multiblock.components.IOwnerState;

import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import net.minecraft.nbt.CompoundTag;
@Mixin(blusunrize.immersiveengineering.common.blocks.multiblocks.logic.AssemblerLogic.State.class)
public abstract class OwnerableStateMixin implements IMultiblockState,IOwnerState<Object> {
	private UUID owner;
	public OwnerableStateMixin() {
	}

	@Override
	public UUID getOwner() {
		return owner;
	}

	@Override
	public void setOwner(UUID owner) {
		this.owner=owner;
	}

	@Override
	public void onOwnerChange(IMultiblockContext<?> ctx) {
		
	}
	@Inject(at=@At("TAIL"),method="writeSaveNBT",remap=false)
	public void fh$writeSaveNBT(CompoundTag nbt,CallbackInfo cbi) {
		if(owner!=null)
			nbt.putUUID("owner", owner);
	}

	@Inject(at=@At("TAIL"),method="readSaveNBT",remap=false)
	public void fh$readSaveNBT(CompoundTag nbt,CallbackInfo cbi) {
		if(nbt.contains("owner"))
			owner=nbt.getUUID("owner");
	}

}

package com.teammoeg.frostedheart.mixin.minecraft;

import java.util.UUID;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.util.IOwnerTile;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;


/**
 * Mixin to set owner for TEs, for research systems.
 * 
 * */
@Mixin(TileEntity.class)
public class TileEntityMixin implements IOwnerTile{
	UUID id;

	@Override
	public UUID getStoredOwner() {
		return id;
	}

	@Override
	public void setStoredOwner(UUID id) {
		this.id=id;
	}
	
	@Inject(at=@At("RETURN"),method="read(Lnet/minecraft/block/BlockState;Lnet/minecraft/nbt/CompoundNBT;)V")
	public void fh$to$read(BlockState bs,CompoundNBT nbt,CallbackInfo cbi) {
		if(nbt.contains("fhowner"))
			id=UUID.fromString(nbt.getString("fhowner"));
	}
	@Inject(at=@At("HEAD"),method="write(Lnet/minecraft/nbt/CompoundNBT;)Lnet/minecraft/nbt/CompoundNBT;")
	public void fh$to$write(CompoundNBT nbt,CallbackInfoReturnable<CompoundNBT> cbi) {
		if(id!=null)
			nbt.putString("fhowner",id.toString());
	}
}

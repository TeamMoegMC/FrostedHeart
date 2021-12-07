package com.teammoeg.frostedheart.mixin.minecraft;

import com.teammoeg.frostedheart.climate.chunkdata.ChunkData;
import net.minecraft.tileentity.BeehiveTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(BeehiveTileEntity.class)
public class BeeHiveMixin extends TileEntity {

	public BeeHiveMixin(TileEntityType<?> tileEntityTypeIn) {
		super(tileEntityTypeIn);
	}
	@Inject(at=@At("HEAD"),method="tick",cancellable=true)
	public void tick(CallbackInfo cbi) {
		if(!world.isRemote&&ChunkData.getTemperature(world, pos)<14)
			cbi.cancel();
	} 
}

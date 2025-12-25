package com.teammoeg.frostedheart.mixin.drawer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityController;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntitySlave;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
@Mixin({BlockEntityController.class,BlockEntitySlave.class})
public class ControllerBlockEntityMixin extends BlockEntity {

	public ControllerBlockEntityMixin(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
		super(pType, pPos, pBlockState);
	}
	/**
	 * @author khjxiaogu
	 * @reason disable automatic interaction with controller and slave block, just like GT
	 * */
	@Inject(at=@At("HEAD"),method="getCapability",cancellable=true,remap=false)
	private void fh$getCapability(Capability cap,Direction side,CallbackInfoReturnable<LazyOptional> cbi) {
		cbi.setReturnValue(LazyOptional.empty());
	}

}

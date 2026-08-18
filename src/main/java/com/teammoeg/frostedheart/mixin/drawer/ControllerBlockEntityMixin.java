/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.mixin.drawer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityController;
import com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntitySlave;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

@Mixin({BlockEntityController.class,BlockEntitySlave.class})
public class ControllerBlockEntityMixin extends BlockEntity {
	@Unique
	private DrawerItemHandlerThrottle.State fh$transferThrottle;

	public ControllerBlockEntityMixin(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
		super(pType, pPos, pBlockState);
	}

	/**
	 * @author khjxiaogu
	 * @reason Rate-limit automatic item interaction with controller and slave blocks.
	 * */
	@Inject(at=@At("RETURN"),method="getCapability",cancellable=true,remap=false)
	private <T> void fh$getCapability(Capability<T> cap, Direction side,
			CallbackInfoReturnable<LazyOptional<T>> cbi) {
		if (cap != ForgeCapabilities.ITEM_HANDLER) {
			cbi.setReturnValue(LazyOptional.empty());
			return;
		}

		LazyOptional<IItemHandler> original = cbi.getReturnValue().cast();
		LazyOptional<IItemHandler> throttled = original.lazyMap(handler ->
				new DrawerItemHandlerThrottle(handler, this::fh$getGameTime, fh$getTransferThrottle()));
		cbi.setReturnValue(throttled.cast());
	}

	@Unique
	private DrawerItemHandlerThrottle.State fh$getTransferThrottle() {
		if (fh$transferThrottle == null)
			fh$transferThrottle = new DrawerItemHandlerThrottle.State(
					() -> FHConfig.SERVER.STORAGE_DRAWERS.inputCooldownTicks.get(),
					() -> FHConfig.SERVER.STORAGE_DRAWERS.outputCooldownTicks.get());
		return fh$transferThrottle;
	}

	@Unique
	private long fh$getGameTime() {
		return level == null ? 0L : level.getGameTime();
	}
}

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

package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import com.teammoeg.frostedheart.content.climate.data.StateTransitionData;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.properties.Property;

@Mixin(BlockStateBase.class)
public abstract class BlockStateBaseMixin_RandomTick extends StateHolder<Block, BlockState> {

	protected BlockStateBaseMixin_RandomTick(Block pOwner, ImmutableMap<Property<?>, Comparable<?>> pValues,
			MapCodec<BlockState> pPropertiesCodec) {
		super(pOwner, pValues, pPropertiesCodec);
	}

	@Inject(at = @At("RETURN"), method = "isRandomlyTicking",cancellable=true)
	public void fh$isRandomlyTicking(CallbackInfoReturnable<Boolean> bool) {
		if(((Object)this) instanceof BlockState bs) {
			StateTransitionData std=StateTransitionData.getData(bs);
			if(std!=null)
				bool.setReturnValue(std.willTransit()||bool.getReturnValueZ());
		}
	}

}

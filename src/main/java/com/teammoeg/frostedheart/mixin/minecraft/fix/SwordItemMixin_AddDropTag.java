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

package com.teammoeg.frostedheart.mixin.minecraft.fix;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(SwordItem.class)
public class SwordItemMixin_AddDropTag extends TieredItem {



	public SwordItemMixin_AddDropTag(Tier pTier, Properties pProperties) {
		super(pTier, pProperties);
		// TODO Auto-generated constructor stub
	}

	@Inject(at = @At("HEAD"), method = "mineBlock", cancellable = true)
	public void fh$mineBlock(ItemStack pStack, Level pLevel, BlockState pState, BlockPos pPos, LivingEntity pEntityLiving, CallbackInfoReturnable<Boolean> cbi) {
		if (pState.is(BlockTags.SWORD_EFFICIENT)) {
			pStack.hurtAndBreak(1, pEntityLiving, (p_43276_) -> {
				p_43276_.broadcastBreakEvent(EquipmentSlot.MAINHAND);
			});
			cbi.setReturnValue(true);
		}

	}

	@Inject(at = @At("HEAD"), method = "isCorrectToolForDrops", cancellable = true)
	public void fh$isCorrectToolForDrops(BlockState pBlock, CallbackInfoReturnable<Boolean> cbi) {
		if (pBlock.is(BlockTags.SWORD_EFFICIENT))
			cbi.setReturnValue(true);
	}
}

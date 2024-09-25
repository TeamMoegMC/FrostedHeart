/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.utility;

import java.util.List;

import com.teammoeg.frostedheart.base.item.FHBaseItem;
import com.teammoeg.frostedheart.content.climate.player.IHeatingEquipment;
import com.teammoeg.frostedheart.content.climate.player.ITempAdjustFood;
import com.teammoeg.frostedheart.util.TranslateUtils;
import com.teammoeg.frostedheart.util.constants.EquipmentCuriosSlotType;

import blusunrize.immersiveengineering.common.util.EnergyHelper;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;

import net.minecraft.world.item.Item.Properties;

public class SteamBottleItem extends FHBaseItem implements IHeatingEquipment, ITempAdjustFood {


    public SteamBottleItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        String stored = this.getEnergyStored(stack) + "/" + this.getMaxEnergyStored(stack);
        tooltip.add(TranslateUtils.translateTooltip("meme.steam_bottle").withStyle(ChatFormatting.GRAY));
        tooltip.add(TranslateUtils.translateTooltip("steam_stored", stored).withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void fillItemCategory(CreativeModeTab group, NonNullList<ItemStack> items) {
        if (this.allowdedIn(group)) {
            ItemStack is = new ItemStack(this);
            this.receiveEnergy(is, this.getMaxEnergyStored(is), false);
            items.add(is);
        }

    }

    @Override
    public float getHeat(ItemStack is, float env) {
        return (float) this.getEnergyStored(is) / 120;
    }


    @Override
    public int getMaxEnergyStored(ItemStack container) {
        return 240;
    }

    /**
     * returns the action that specifies what animation to play when the items is being used
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }


    /**
     * How long it takes to use or consume an item
     */
    @Override
    public int getUseDuration(ItemStack stack) {
        return 16;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level worldIn, Player playerIn) {
        super.onCraftedBy(stack, worldIn, playerIn);
        this.receiveEnergy(stack, 240, false);
    }

    /**
     * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see
     * {@link #onItemUse}.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        playerIn.startUsingItem(handIn);
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, playerIn.getItemInHand(handIn));
    }

    /**
     * Called when the player finishes using this Item (E.g. finishes eating.). Not called when the player stops using
     * the Item before the action is complete.
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level worldIn, LivingEntity entityLiving) {
        Player entityplayer = entityLiving instanceof Player ? (Player) entityLiving : null;
        if (entityplayer == null || !entityplayer.abilities.instabuild) {
            stack.shrink(1);
        }

        if (entityplayer instanceof ServerPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer) entityplayer, stack);
        }

        if (entityplayer != null) {
            entityplayer.awardStat(Stats.ITEM_USED.get(this));
        }

        if (entityplayer == null || !entityplayer.abilities.instabuild) {
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }

            if (entityplayer != null) {
                entityplayer.inventory.add(new ItemStack(Items.GLASS_BOTTLE));
            }
        }

        return stack;
    }

	@Override
	public float getEffectiveTempAdded(EquipmentCuriosSlotType slot, ItemStack stack, float effectiveTemp,
			float bodyTemp) {
		if(slot==null)return 12.5f;
		
		return this.extractEnergy(stack, 3, false) / 0.24f;
	}

}

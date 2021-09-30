/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.content.heating;

import blusunrize.immersiveengineering.common.util.EnergyHelper;
import com.teammoeg.frostedheart.climate.IHeatingEquipment;
import com.teammoeg.frostedheart.climate.ITempAdjustFood;
import com.teammoeg.frostedheart.base.item.FHBaseItem;
import com.teammoeg.frostedheart.util.FHDamageSources;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.UseAction;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import java.util.List;

public class SteamBottleItem extends FHBaseItem implements IHeatingEquipment, ITempAdjustFood, EnergyHelper.IIEEnergyItem {


    public SteamBottleItem(String name, Properties properties) {
        super(name, properties);
    }

    /**
     * Called when the player finishes using this Item (E.g. finishes eating.). Not called when the player stops using
     * the Item before the action is complete.
     */
    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
        PlayerEntity entityplayer = entityLiving instanceof PlayerEntity ? (PlayerEntity) entityLiving : null;
        if (entityplayer == null || !entityplayer.abilities.isCreativeMode) {
            stack.shrink(1);
        }

        if (entityplayer instanceof ServerPlayerEntity) {
            CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayerEntity) entityplayer, stack);
            entityplayer.attackEntityFrom(FHDamageSources.HYPERTHERMIA, (this.getEnergyStored(stack) / 60) + 2);
        }

        if (entityplayer != null) {
            entityplayer.addStat(Stats.ITEM_USED.get(this));
        }

        if (entityplayer == null || !entityplayer.abilities.isCreativeMode) {
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }

            if (entityplayer != null) {
                entityplayer.inventory.addItemStackToInventory(new ItemStack(Items.GLASS_BOTTLE));
            }
        }

        return stack;
    }

    @Override
    public void addInformation(ItemStack stack, World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        String stored = this.getEnergyStored(stack) + "/" + this.getMaxEnergyStored(stack);
        tooltip.add(new TranslationTextComponent("tooltip.frostedheart.meme.steam_bottle").mergeStyle(TextFormatting.GRAY));
        tooltip.add(new TranslationTextComponent("frostedheart.desc.steamStored", stored).mergeStyle(TextFormatting.GOLD));
    }

    /**
     * How long it takes to use or consume an item
     */
    @Override
    public int getUseDuration(ItemStack stack) {
        return 16;
    }

    @Override
    public void onCreated(ItemStack stack, World worldIn, PlayerEntity playerIn) {
        super.onCreated(stack, worldIn, playerIn);
        this.receiveEnergy(stack, 240, false);
    }


	/**
     * returns the action that specifies what animation to play when the items is being used
     */
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }

    /**
     * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see
     * {@link #onItemUse}.
     */
    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        playerIn.setActiveHand(handIn);
        return new ActionResult<>(ActionResultType.SUCCESS, playerIn.getHeldItem(handIn));
    }

    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        if (this.isInGroup(group)) {
            ItemStack is = new ItemStack(this);
            this.receiveEnergy(is, this.getMaxEnergyStored(is), false);
            items.add(is);
        }

    }

    @Override
    public int getMaxEnergyStored(ItemStack container) {
        return 240;
    }

    @Override
    public float getHeat(ItemStack is) {
        return this.getEnergyStored(is) / 120;
    }

    @Override
    public float compute(ItemStack stack, float bodyTemp, float environmentTemp) {
        return bodyTemp + this.extractEnergy(stack, 3, false) / 120;
    }

	@Override
	public float getMax(ItemStack stack) {
		return 0.025F;
	}

}

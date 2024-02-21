/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.temperature.handstoves;

import java.util.List;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.base.item.FHBaseItem;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.climate.player.EquipmentCuriosSlotType;
import com.teammoeg.frostedheart.climate.player.IHeatingEquipment;
import com.teammoeg.frostedheart.util.FHUtils;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.tags.ITag;
import net.minecraft.tags.TagCollectionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class CoalHandStove extends FHBaseItem implements IHeatingEquipment {
    public final static int max_fuel = 800;

    ResourceLocation ashitem = new ResourceLocation("frostedheart", "ash");

    public static int getAshAmount(ItemStack is) {
        return is.getOrCreateTag().getInt("ash");
    }

    public static int getFuelAmount(ItemStack is) {
        return is.getOrCreateTag().getInt("fuel");
    }

    public static void setAshAmount(ItemStack is, int v) {
        is.getOrCreateTag().putInt("ash", v);
        if (v >= max_fuel)
            is.getTag().putInt("CustomModelData", 2);
    }

    public static void setFuelAmount(ItemStack is, int v) {
        is.getOrCreateTag().putInt("fuel", v);
        if (v < 2)
            is.getTag().putInt("CustomModelData", 0);
        else
            is.getTag().putInt("CustomModelData", 1);
    }

    public CoalHandStove(Properties properties) {
        super(properties);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        list.add(GuiUtils.translateTooltip("handstove.add_fuel").mergeStyle(TextFormatting.GRAY));
        if (getAshAmount(stack) >= 800)
            list.add(GuiUtils.translateTooltip("handstove.trash_ash").mergeStyle(TextFormatting.RED));
        list.add(GuiUtils.translateTooltip("handstove.fuel", getFuelAmount(stack) / 2).mergeStyle(TextFormatting.GRAY));
    }


    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return getFuelAmount(stack) * 1.0D / max_fuel;
    }


    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 40;
    }


    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);
        ActionResult<ItemStack> FAIL = new ActionResult<>(ActionResultType.FAIL, stack);
        if (getAshAmount(playerIn.getHeldItem(handIn)) >= 800) {
            playerIn.setActiveHand(handIn);
            return new ActionResult<>(ActionResultType.SUCCESS, stack);
        }
        return FAIL;
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
        int ash = getAshAmount(stack);
        if (ash >= 800) {
            ITag<Item> item = TagCollectionManager.getManager().getItemTags().get(ashitem);
            setAshAmount(stack, ash - 800);
            if (getFuelAmount(stack) < 2)
                stack.getTag().putInt("CustomModelData", 0);
            else
                stack.getTag().putInt("CustomModelData", 1);
            if (item != null && entityLiving instanceof PlayerEntity && !item.getAllElements().isEmpty()) {
                ItemStack ret = new ItemStack(item.getAllElements().get(0));
                FHUtils.giveItem((PlayerEntity) entityLiving, ret);
            }
        }
        return stack;
    }

	@Override
	public float getEffectiveTempAdded(EquipmentCuriosSlotType slot, ItemStack stack, float effectiveTemp, float bodyTemp) {
		if(slot==null) {
			return getFuelAmount(stack) > 0 ? 7 : 0;
		}else if(slot.isHand()) {
	        int fuel = getFuelAmount(stack);
	        if (fuel >= 2) {
	            int ash = getAshAmount(stack);
	            if (ash <= 800) {
	                fuel--;
	                ash++;
	                setFuelAmount(stack, fuel);
	                setAshAmount(stack, ash);
	                return 7;
	            }
	        }
	        return 0;
		}
		return 0;
	}
}

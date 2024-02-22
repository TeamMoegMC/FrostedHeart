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

package com.teammoeg.frostedheart.content.temperature;

import java.util.List;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.item.FHBaseItem;
import com.teammoeg.frostedheart.climate.player.EquipmentCuriosSlotType;
import com.teammoeg.frostedheart.climate.player.IHeatingEquipment;
import com.teammoeg.frostedheart.util.client.GuiUtils;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class MushroomBed extends FHBaseItem implements IHeatingEquipment {
    public static final ResourceLocation ktag = new ResourceLocation(FHMain.MODID, "knife");

    Item resultType;

    public MushroomBed( Item resultType, Properties properties) {
        super(properties);
        this.resultType = resultType;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (stack.getDamage() > 0)
            list.add(GuiUtils.translateTooltip("meme.mushroom").mergeStyle(TextFormatting.GRAY));
        else
            list.add(GuiUtils.translateTooltip("mushroom").mergeStyle(TextFormatting.GRAY));
    }



    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        if (this.isInGroup(group)) {
            ItemStack is = new ItemStack(this);
            items.add(is);
            is.setDamage(is.getMaxDamage());
            items.add(is);
        }
    }
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }


    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack stack = playerIn.getHeldItem(handIn);
        ActionResult<ItemStack> FAIL = new ActionResult<>(ActionResultType.FAIL, stack);
        if (stack.getDamage() > 0)
            return FAIL;


        Hand otherHand = handIn == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        if (playerIn.getHeldItem(otherHand).getItem().getTags().contains(ktag)) {
            playerIn.setActiveHand(handIn);
            return new ActionResult<>(ActionResultType.SUCCESS, stack);
        }
        return FAIL;
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {

        if (stack.getDamage() == 0) {
            Hand otherHand = entityLiving.getActiveHand() == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
            entityLiving.getHeldItem(otherHand).damageItem(1, entityLiving, (player2) -> player2.sendBreakAnimation(otherHand));
            return new ItemStack(resultType, 10);

        }
        return stack;
    }

	@Override
	public float getEffectiveTempAdded(EquipmentCuriosSlotType slot, ItemStack stack, float effectiveTemp, float bodyTemp) {
		if(slot==null)
			return 0.5f;
        if (stack.getDamage() > 0) {
            if (bodyTemp > -1) {
                this.setDamage(stack, this.getDamage(stack) - 1);
                return 0.5f;
            }
        }
		return 0;
	}

}

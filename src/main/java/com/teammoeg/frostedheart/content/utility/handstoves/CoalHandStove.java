/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.utility.handstoves;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.datafixers.util.Either;
import com.teammoeg.frostedheart.item.FHBaseItem;
import com.teammoeg.frostedheart.content.climate.player.IHeatingEquipment;
import com.teammoeg.frostedheart.util.client.Lang;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.content.climate.player.EquipmentSlotType;
import com.teammoeg.frostedheart.content.climate.player.EquipmentSlotType.SlotKey;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.type.ISlotType;

public class CoalHandStove extends FHBaseItem implements IHeatingEquipment {
    public final static int max_fuel = 800;

    TagKey<Item> ashitem=ItemTags.create(new ResourceLocation("frostedheart", "ash"));
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
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag flag) {
//        list.add(Lang.translateTooltip("handstove.add_fuel").withStyle(ChatFormatting.GRAY));
        if (getAshAmount(stack) >= 800)
            list.add(Lang.translateTooltip("handstove.trash_ash").withStyle(ChatFormatting.RED));
        list.add(Lang.translateTooltip("handstove.fuel", getFuelAmount(stack) / 2).withStyle(ChatFormatting.GRAY));
    }


    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) (getFuelAmount(stack) * 13.0D / max_fuel);
    }


    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
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
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack stack = playerIn.getItemInHand(handIn);
        InteractionResultHolder<ItemStack> FAIL = new InteractionResultHolder<>(InteractionResult.FAIL, stack);
        if (getAshAmount(playerIn.getItemInHand(handIn)) >= 800) {
            playerIn.startUsingItem(handIn);
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }
        return FAIL;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level worldIn, LivingEntity entityLiving) {
        int ash = getAshAmount(stack);
        if (ash >= 800) {
            Iterator<Item> item=ForgeRegistries.ITEMS.tags().getTag(ashitem).iterator();
            setAshAmount(stack, ash - 800);
            if (getFuelAmount(stack) < 2)
                stack.getTag().putInt("CustomModelData", 0);
            else
                stack.getTag().putInt("CustomModelData", 1);
            if (item.hasNext() && entityLiving instanceof Player ) {
                ItemStack ret = new ItemStack(item.next());
                CUtils.giveItem((Player) entityLiving, ret);
            }
        }
        return stack;
    }

	@Override
	public float getEffectiveTempAdded(Either<ISlotType,SlotKey> slot, ItemStack stack, float effectiveTemp, float bodyTemp) {
		if(slot==null) {
			return getFuelAmount(stack) > 0 ? 7 : 0;
		}else if(slot.map(t->false, t->t.isHand())) {
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

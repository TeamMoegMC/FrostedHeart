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

package com.teammoeg.frostedheart.content.utility;

import java.util.List;

import com.teammoeg.frostedheart.item.FHBaseItem;
import com.teammoeg.frostedheart.util.Lang;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.climate.TemperatureDisplayHelper;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;

public class ThermometerItem extends FHBaseItem {


    public ThermometerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        tooltip.add(Lang.translateTooltip("thermometer.usage").withStyle(ChatFormatting.GRAY));
        tooltip.add(Lang.translateTooltip("meme.thermometerbody").withStyle(ChatFormatting.GRAY));
    }

    public int getTemperature(ServerPlayer p) {
    	 PlayerTemperatureData ptd=PlayerTemperatureData.getCapability(p).orElse(null);
    	 if(ptd!=null)
    		 return (int) (ptd.getCoreBodyTemp()* 10);
    	 return 0;
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
        return 100;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        playerIn.displayClientMessage(Lang.translateMessage("thermometer.testing"), true);
        playerIn.startUsingItem(handIn);
        if (playerIn instanceof ServerPlayer && playerIn.getAbilities().instabuild) {
            TemperatureDisplayHelper.sendTemperature((ServerPlayer) playerIn, "info.thermometerbody", getTemperature((ServerPlayer) playerIn) / 10f + 37f);
        }
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, playerIn.getItemInHand(handIn));
    }

    /**
     * Called when the player finishes using this Item (E.g. finishes eating.). Not called when the player stops using
     * the Item before the action is complete.
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level worldIn, LivingEntity entityLiving) {
        if (worldIn.isClientSide) return stack;
        if (entityLiving instanceof ServerPlayer) {
            TemperatureDisplayHelper.sendTemperature((ServerPlayer) entityLiving, "info.thermometerbody", getTemperature((ServerPlayer) entityLiving) / 10f + 37f);
        }

        return stack;
    }

}

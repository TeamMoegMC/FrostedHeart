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

package com.teammoeg.frostedheart.content.temperature;

import java.util.List;

import com.teammoeg.frostedheart.base.item.FHBaseItem;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.climate.chunkdata.ChunkHeatData;
import com.teammoeg.frostedheart.network.climate.FHTemperatureDisplayPacket;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class SoilThermometer extends FHBaseItem {
    public SoilThermometer(String name, Properties properties) {
        super(name, properties);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
    	playerIn.sendStatusMessage(GuiUtils.translateMessage("thermometer.testing"),true);
        playerIn.setActiveHand(handIn);
        if (playerIn instanceof ServerPlayerEntity&&playerIn.abilities.isCreativeMode) {
            BlockRayTraceResult brtr = rayTrace(worldIn, playerIn, FluidMode.ANY);
            if (brtr.getType() != Type.MISS)
            	FHTemperatureDisplayPacket.send((ServerPlayerEntity)playerIn,
            			"info.soil_thermometerbody", (int)(ChunkHeatData.getTemperature(playerIn.world, brtr.getPos())*10)/10f);
        }
        return new ActionResult<>(ActionResultType.SUCCESS, playerIn.getHeldItem(handIn));
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
        if (worldIn.isRemote) return stack;
        PlayerEntity entityplayer = entityLiving instanceof PlayerEntity ? (PlayerEntity) entityLiving : null;
        if (entityplayer instanceof ServerPlayerEntity) {
            BlockRayTraceResult brtr = rayTrace(worldIn, entityplayer, FluidMode.ANY);
            if (brtr.getType() == Type.MISS) return stack;
            FHTemperatureDisplayPacket.send((ServerPlayerEntity)entityLiving,
        			"info.soil_thermometerbody", (int)(ChunkHeatData.getTemperature(entityLiving.world, brtr.getPos())*10)/10f);
        }
        return stack;
    }
    @Override
    public void addInformation(ItemStack stack, World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
    	tooltip.add(GuiUtils.translateTooltip("thermometer.usage").mergeStyle(TextFormatting.GRAY));
    }
    @Override
    public int getUseDuration(ItemStack stack) {
        return 100;
    }

    /**
     * returns the action that specifies what animation to play when the items is being used
     */
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPEAR;
    }
}

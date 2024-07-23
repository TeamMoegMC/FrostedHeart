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

package com.teammoeg.frostedheart.content.steamenergy.debug;

import com.teammoeg.frostedheart.FHCapabilities;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.steamenergy.EnergyNetworkProvider;
import com.teammoeg.frostedheart.content.steamenergy.HeatHandler;
import com.teammoeg.frostedheart.util.TranslateUtils;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import net.minecraft.item.Item.Properties;

public class HeatDebugItem extends Item {
    public HeatDebugItem() {
        super(new Properties().stacksTo(1).setNoRepair().tab(FHMain.itemGroup));
    }

    //Dont add to creative tag
    @Override
    public void fillItemCategory(ItemGroup group, NonNullList<ItemStack> items) {
    }

    public UseAction getUseAnimation(ItemStack stack) {
        return UseAction.NONE;
    }

    public int getUseDuration(ItemStack stack) {
        return 1;
    }

    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {

        BlockRayTraceResult raytraceresult = getPlayerPOVHitResult(worldIn, playerIn, RayTraceContext.FluidMode.SOURCE_ONLY);
        ItemStack itemstack = playerIn.getItemInHand(handIn);
        if (worldIn.isClientSide) return ActionResult.success(itemstack);
        if (raytraceresult.getType() == RayTraceResult.Type.BLOCK) {
        	if(playerIn instanceof ServerPlayerEntity) {
	            BlockPos blockpos = raytraceresult.getBlockPos();
	            TileEntity te = Utils.getExistingTileEntity(worldIn, blockpos);
	            if (te instanceof EnergyNetworkProvider) {
	            	if(((EnergyNetworkProvider) te).getNetwork()!=null)
	            		HeatHandler.openHeatScreen((ServerPlayerEntity) playerIn, ((EnergyNetworkProvider) te).getNetwork());
	            	else playerIn.sendMessage(TranslateUtils.str("EnergyNetwork " + ((EnergyNetworkProvider) te).getNetwork()), playerIn.getUUID());
	            }else if(te!=null) {
	            	playerIn.sendMessage(TranslateUtils.str("EnergyEndpoint "+te.getCapability(FHCapabilities.HEAT_EP.capability(), raytraceresult.getDirection()).orElse(null)), playerIn.getUUID());
	            }
            }
            return ActionResult.success(itemstack);
        }
        return ActionResult.fail(itemstack);
    }
}

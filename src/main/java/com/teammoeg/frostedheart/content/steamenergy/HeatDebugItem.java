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

package com.teammoeg.frostedheart.content.steamenergy;

import com.teammoeg.frostedheart.FHMain;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.entity.player.PlayerEntity;
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
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

public class HeatDebugItem extends Item {
    public HeatDebugItem() {
        super(new Properties().maxStackSize(1).setNoRepair().group(FHMain.itemGroup));
    }

    //Dont add to creative tag
    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
    }

    public UseAction getUseAction(ItemStack stack) {
        return UseAction.NONE;
    }

    public int getUseDuration(ItemStack stack) {
        return 1;
    }

    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {

        RayTraceResult raytraceresult = rayTrace(worldIn, playerIn, RayTraceContext.FluidMode.SOURCE_ONLY);
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        if (worldIn.isRemote) return ActionResult.resultSuccess(itemstack);
        if (raytraceresult.getType() == RayTraceResult.Type.BLOCK) {
            BlockPos blockpos = ((BlockRayTraceResult) raytraceresult).getPos();
            TileEntity te = Utils.getExistingTileEntity(worldIn, blockpos);
            if (te instanceof HeatController) {
                playerIn.sendMessage(new StringTextComponent("HeatProvider network=" + ((HeatController) te).getNetwork()), playerIn.getUniqueID());
            } else if (te instanceof EnergyNetworkProvider) {
                playerIn.sendMessage(new StringTextComponent("EnergyNetworkProvider network=" + ((EnergyNetworkProvider) te).getNetwork()), playerIn.getUniqueID());
            } else if (te instanceof INetworkConsumer) {
                if (((INetworkConsumer) te).getHolder() != null)
                    playerIn.sendMessage(new StringTextComponent("EnergyNetworkConsumer data=" + ((INetworkConsumer) te).getHolder()), playerIn.getUniqueID());
            }
            return ActionResult.resultSuccess(itemstack);
        }
        return ActionResult.resultFail(itemstack);
    }
}

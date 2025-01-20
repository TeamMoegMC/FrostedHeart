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

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.chorda.util.lang.Components;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.content.steamenergy.HeatNetworkProvider;
import com.teammoeg.frostedheart.content.steamenergy.HeatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class HeatDebugItem extends Item {
    public HeatDebugItem() {
        super(new Properties().stacksTo(1).setNoRepair());
    }


    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    public int getUseDuration(ItemStack stack) {
        return 1;
    }

    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {

        BlockHitResult raytraceresult = getPlayerPOVHitResult(worldIn, playerIn, ClipContext.Fluid.SOURCE_ONLY);
        ItemStack itemstack = playerIn.getItemInHand(handIn);
        if (worldIn.isClientSide) return InteractionResultHolder.success(itemstack);
        if (raytraceresult.getType() == HitResult.Type.BLOCK) {
            if (playerIn instanceof ServerPlayer) {
                BlockPos blockpos = raytraceresult.getBlockPos();
                BlockEntity te = Utils.getExistingTileEntity(worldIn, blockpos);
                if (te instanceof HeatNetworkProvider) {
                    if (((HeatNetworkProvider) te).getNetwork() != null)
                        HeatHandler.openHeatScreen((ServerPlayer) playerIn, ((HeatNetworkProvider) te).getNetwork());
                    else
                        playerIn.sendSystemMessage(Components.str("EnergyNetwork " + ((HeatNetworkProvider) te).getNetwork()));
                } else if (te != null) {
                    playerIn.sendSystemMessage(Components.str("EnergyEndpoint " + te.getCapability(FHCapabilities.HEAT_EP.capability(), raytraceresult.getDirection()).orElse(null)));
                }
            }
            return InteractionResultHolder.success(itemstack);
        }
        return InteractionResultHolder.fail(itemstack);
    }
}

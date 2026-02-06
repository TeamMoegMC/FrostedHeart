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

package com.teammoeg.frostedheart.content.water.event;

import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.water.network.PlayerDrinkWaterMessage;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
public class WaterClientEvents {

    @SubscribeEvent
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack heldItem = event.getItemStack();
        Player player = event.getEntity();
        //drink water block
        if (heldItem.isEmpty()  && player.getPose() == Pose.CROUCHING) {
        	BlockPos pos=event.getHitVec().getBlockPos().offset(event.getFace().getNormal());
        	if(!event.getLevel().getFluidState(pos).isEmpty())
        		FHNetwork.INSTANCE.sendToServer(new PlayerDrinkWaterMessage(pos));
        }
    }

    @SubscribeEvent
    public static void onPlayerRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        Player player = event.getEntity();
        Level level = event.getLevel();
        //drink water block
        HitResult hitresult = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (player.getPose() == Pose.CROUCHING && hitresult.getType() == HitResult.Type.BLOCK) {
        
        	BlockPos pos=CUtils.vec2Pos(hitresult.getLocation());
        	if(!event.getLevel().getFluidState(pos).isEmpty())
        		FHNetwork.INSTANCE.sendToServer(new PlayerDrinkWaterMessage(pos));
        }
    }

}

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

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.client.FHKeyMappings;
import com.teammoeg.frostedheart.content.water.network.PlayerDrinkWaterMessage;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE,value=Dist.CLIENT)
public class WaterClientEvents {

    @SubscribeEvent
    public static void onClientKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_PRESS && FHKeyMappings.key_drink.get().consumeClick()) {
            if (ClientUtils.getMc().hitResult instanceof BlockHitResult bhr) {
                var pos = bhr.getBlockPos();
                if (!ClientUtils.getWorld().getFluidState(pos).is(Fluids.WATER)) {
                    pos = bhr.getBlockPos().relative(bhr.getDirection());
                }
                if (ClientUtils.getWorld().getFluidState(pos).is(Fluids.WATER)) {
                    ClientUtils.getLocalPlayer().playSound(SoundEvents.GENERIC_DRINK, 0.2F, 1);
                    FHNetwork.INSTANCE.sendToServer(new PlayerDrinkWaterMessage(pos));
                }
            }
        }
    }

//    @SubscribeEvent
//    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
//        ItemStack heldItem = event.getItemStack();
//        Player player = event.getEntity();
//        //drink water block
//        if (heldItem.isEmpty()  && player.getPose() == Pose.CROUCHING) {
//        	BlockPos pos=event.getHitVec().getBlockPos().offset(event.getFace().getNormal());
//        	if(!event.getLevel().getFluidState(pos).isEmpty()) {
//                FHNetwork.INSTANCE.sendToServer(new PlayerDrinkWaterMessage(pos));
//                player.playSound(SoundEvents.GENERIC_DRINK, 0.2F, 1);
//            }
//        }
//    }
//
//    @SubscribeEvent
//    public static void onPlayerRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
//        Player player = event.getEntity();
//        Level level = event.getLevel();
//        //drink water block
//        HitResult hitresult = Item.getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
//        if (player.getPose() == Pose.CROUCHING && hitresult.getType() == HitResult.Type.BLOCK) {
//
//        	BlockPos pos=CUtils.vec2Pos(hitresult.getLocation());
//        	if(!event.getLevel().getFluidState(pos).isEmpty())
//        		FHNetwork.INSTANCE.sendToServer(new PlayerDrinkWaterMessage(pos));
//        }
//    }

}

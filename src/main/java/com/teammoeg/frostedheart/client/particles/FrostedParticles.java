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

package com.teammoeg.frostedheart.client.particles;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.climate.player.Temperature;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FrostedParticles {

    /**
     * Simulate breath particles when the player is in a cold environment
     *
     * @param event
     */
    @SubscribeEvent
    public static void addBreathParticles(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.CLIENT && event.phase == TickEvent.Phase.START
                && event.player instanceof ClientPlayerEntity) {
            ClientPlayerEntity player = (ClientPlayerEntity) event.player;
            if (!player.isSpectator() && !player.isCreative() && player.world != null) {
                if (player.ticksExisted % 60 <= 3) {
                    float envTemp = Temperature.getEnv(player);
                    if (envTemp < -10.0F) {
                        // get the player's facing vector and make the particle spawn in front of the player
                        double x = player.getPosX() + player.getLookVec().x * 0.3D;
                        double z = player.getPosZ() + player.getLookVec().z * 0.3D;
                        double y = player.getPosY() + 1.3D;
                        // the speed of the particle is based on the player's facing, so it looks like it's coming from their mouth
                        double xSpeed = player.getLookVec().x * 0.03D;
                        double ySpeed = player.getLookVec().y * 0.03D;
                        double zSpeed = player.getLookVec().z * 0.03D;
                        // apply the player's motion to the particle
                        xSpeed += player.getMotion().x;
                        ySpeed += player.getMotion().y;
                        zSpeed += player.getMotion().z;
                        player.world.addParticle(FHParticleTypes.BREATH.get(), x, y, z, xSpeed, ySpeed, zSpeed);
                    }
                }
            }
        }
    }
}

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

package com.teammoeg.frostedheart.content.climate.event;

import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.reference.FHParticleTypes;
import com.teammoeg.frostedheart.bootstrap.reference.FHSoundEvents;
import com.teammoeg.frostedheart.content.climate.block.generator.GeneratorScreen;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.climate.render.TemperatureGoogleRenderer;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClimateClientEvents {

    /**
     * Simulate breath particles when the player is in a cold environment
     */
    @SubscribeEvent
    public static void addBreathParticles(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.CLIENT && FHConfig.CLIENT.enableBreathParticle.get() && event.phase == TickEvent.Phase.START
                && event.player instanceof LocalPlayer player) {
            if (!player.isSpectator() && !player.isCreative() && player.level() != null) {
                if (player.tickCount % 60 <= 3) {
                    PlayerTemperatureData.getCapability(player).ifPresent(data -> {
                        float envTemp = data.getEnvTemp();
                        if (envTemp < -10.0F) {
                            // get the player's facing vector and make the particle spawn in front of the player
                            double x = player.getX() + player.getLookAngle().x * 0.3D;
                            double z = player.getZ() + player.getLookAngle().z * 0.3D;
                            double y = player.getY() + 1.3D;
                            // the speed of the particle is based on the player's facing, so it looks like it's coming from their mouth
                            double xSpeed = player.getLookAngle().x * 0.03D;
                            double ySpeed = player.getLookAngle().y * 0.03D;
                            double zSpeed = player.getLookAngle().z * 0.03D;
                            // apply the player's motion to the particle
                            xSpeed += player.getDeltaMovement().x;
                            ySpeed += player.getDeltaMovement().y;
                            zSpeed += player.getDeltaMovement().z;
                            player.level().addParticle(FHParticleTypes.BREATH.get(), x, y, z, xSpeed, ySpeed, zSpeed);
                        }
                    });
                }
            }
        }
    }
    @SubscribeEvent
    public static void tickGeneratorContainer(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.CLIENT &&  event.phase == TickEvent.Phase.END) {
        	if(ClientUtils.getMc().screen instanceof GeneratorScreen gs) {
        		if(event.player.tickCount%10==0)
        			gs.updateTooltip();
        	}
            
        }
    }
    static int forstedSoundCd;
    /**
     * Play ice cracking sound when player's body temperature transitions across integer threshold.
     */
    @SubscribeEvent
    public static void playFrostedSound(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.CLIENT && event.phase == TickEvent.Phase.START
                && event.player instanceof LocalPlayer) {
            LocalPlayer player = (LocalPlayer) event.player;
            if(forstedSoundCd>0)
            	forstedSoundCd--;
            if (!player.isSpectator() && !player.isCreative() && player.level() != null&&forstedSoundCd>0) {
            	
            	PlayerTemperatureData.getCapability(player).ifPresent(ptd -> {
                    float prevTemp = ptd.smoothedBodyPrev;
                    float currTemp = ptd.smoothedBody;
                    // play sound if currTemp transitions across integer threshold
                    if (currTemp <= 0.5F && Mth.floor(prevTemp - 0.5F) != Mth.floor(currTemp - 0.5F)) {
                        player.level().playSound(player, player.blockPosition(), FHSoundEvents.ICE_CRACKING.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
                        forstedSoundCd=20;
                    }
                });

            }
        }
    }
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
    	forstedSoundCd=0;
    }

    @Mod.EventBusSubscriber(modid = FHMain.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModBusEvents {
        @SubscribeEvent
        public static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAboveAll("temperature_google_info", TemperatureGoogleRenderer.OVERLAY);
        }
    }
    

}

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

package com.teammoeg.frostedheart.client.sounds;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.FHSounds;
import com.teammoeg.frostedheart.climate.player.Temperature;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FrostedSounds {
    /**
     * Play ice cracking sound when player's body temperature transitions across integer threshold.
     */
    @SubscribeEvent
    public static void playFrostedSound(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.CLIENT && event.phase == TickEvent.Phase.START
                && event.player instanceof ClientPlayerEntity) {
            ClientPlayerEntity player = (ClientPlayerEntity) event.player;
            if (!player.isSpectator() && !player.isCreative() && player.world != null) {
                float prevTemp = Temperature.getBodySmoothedPrevious(player);
                float currTemp = Temperature.getBodySmoothed(player);
                // play sound if currTemp transitions across integer threshold
                if (MathHelper.floor(prevTemp) != MathHelper.floor(currTemp))
                    player.world.playSound(player, player.getPosition(), FHSounds.ICE_CRACKING.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
        }
    }
}

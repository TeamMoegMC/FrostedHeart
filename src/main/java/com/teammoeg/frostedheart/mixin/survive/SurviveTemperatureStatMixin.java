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

package com.teammoeg.frostedheart.mixin.survive;

import com.stereowalker.survive.util.TemperatureStats;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TemperatureStats.class)
public class SurviveTemperatureStatMixin {
    /**
     * @author khjxiaogu
     * @reason overwrite
     */
    @Overwrite(remap = false)
    private boolean addTemperature(ServerPlayerEntity player, double temperature) {
        return true;
    }

    /**
     * @author khjxiaogu
     * @reason overwrite
     */
    @Overwrite(remap = false)
    public void tick(ServerPlayerEntity player) {

    }

    /**
     * @author khjxiaogu
     * @reason overwrite
     */
    @Overwrite(remap = false)
    @SubscribeEvent
    public static void tickTemperature(LivingUpdateEvent event) {

    }
}

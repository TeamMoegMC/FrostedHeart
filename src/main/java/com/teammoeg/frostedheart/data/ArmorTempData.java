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

package com.teammoeg.frostedheart.data;

import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.climate.IWarmKeepingEquipment;
import com.teammoeg.frostedheart.util.FHEffects;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.biome.Biome.RainType;

public class ArmorTempData extends JsonDataHolder implements IWarmKeepingEquipment {

    public ArmorTempData(JsonObject data) {
        super(data);
    }

    @Override
    public float getFactor(ServerPlayerEntity pe, ItemStack stack) {
        float base = this.getFloatOrDefault("factor", 0F);
        if(pe==null)return base;
        if (pe.isBurning())
            base += this.getFloatOrDefault("fire", 0F);
        if (pe.isInWaterOrBubbleColumn())
            base += this.getFloatOrDefault("water", 0F);
        else if (pe.isInWaterRainOrBubbleColumn()) {
            if (pe.getServerWorld().getBiome(pe.getPosition()).getPrecipitation() == RainType.SNOW)
                base += this.getFloatOrDefault("snow", 0F);
            else
                base += this.getFloatOrDefault("rain", 0F);
        }
        if (pe.isPotionActive(FHEffects.WET)) {//further implement wet
            base += this.getFloatOrDefault("wet", 0F);
        }
        float min = this.getFloatOrDefault("min",0F);
        if (base < min) {
            base = min;
        } else {
            float max = this.getFloatOrDefault("max", 1F);
            if (base > max)
                base = max;

        }
        return base;
    }

}

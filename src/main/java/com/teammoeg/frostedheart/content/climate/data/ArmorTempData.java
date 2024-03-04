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

package com.teammoeg.frostedheart.content.climate.data;

import com.google.gson.JsonObject;

public class ArmorTempData extends JsonDataHolder {

    public ArmorTempData(JsonObject data) {
        super(data);
    }
    public float getInsulation() {
    	return this.getFloatOrDefault("factor", 0F);
    }
    public float getHeatProof() {
    	return this.getFloatOrDefault("heat_proof", 0F);
    }
    public float getColdProof() {
    	return this.getFloatOrDefault("wind_proof", 0F);
    }
  /*  @Override
    public float getFactor(ServerPlayerEntity pe, ItemStack stack) {
        float base = this.getFloatOrDefault("factor", 0F);
        if (pe == null) return base;
        if (pe.isBurning())
            base += this.getFloatOrDefault("fire", 0F);
        if (pe.isInWater())//does not apply twice
            base += this.getFloatOrDefault("water", 0F);
        else if (pe.isPotionActive(FHEffects.WET.get())) {
            base += this.getFloatOrDefault("wet", 0F);
        }
        if (FHUtils.isRainingAt(pe.getPosition(), pe.world)) {
//            if (pe.getServerWorld().getBiome(pe.getPosition()).getPrecipitation() == Biome.RainType.SNOW)
            base += this.getFloatOrDefault("snow", 0F);
//            else
//                base += this.getFloatOrDefault("rain", 0F);
        }

        float min = this.getFloatOrDefault("min", 0F);
        if (base < min) {
            base = min;
        } else {
            float max = this.getFloatOrDefault("max", 1F);
            if (base > max)
                base = max;

        }
        return base;
    }*/
}

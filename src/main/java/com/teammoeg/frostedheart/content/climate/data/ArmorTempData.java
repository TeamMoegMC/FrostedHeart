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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.util.io.CodecUtil;

public class ArmorTempData {
	public static final MapCodec<ArmorTempData> CODEC=RecordCodecBuilder.mapCodec(t->t.group(
		CodecUtil.defaultValue(Codec.FLOAT,0f).fieldOf("factor").forGetter(o->o.insulation),
		CodecUtil.defaultValue(Codec.FLOAT,0f).fieldOf("heat_proof").forGetter(o->o.heat_proof),
		CodecUtil.defaultValue(Codec.FLOAT,0f).fieldOf("wind_proof").forGetter(o->o.wind_proof)).apply(t, ArmorTempData::new));
	float insulation;
	float heat_proof;
	float wind_proof;

    public ArmorTempData(float insulation, float heat_proof, float wind_proof) {
		this.insulation = insulation;
		this.heat_proof = heat_proof;
		this.wind_proof = wind_proof;
	}
	public float getInsulation() {
    	return insulation;
    }
    public float getHeatProof() {
    	return heat_proof;
    }
    public float getColdProof() {
    	return wind_proof;
    }
  /*  @Override
    public float getFactor(ServerPlayerEntity pe, ItemStack stack) {
        float base = this.getFloatOrDefault("factor", 0F);
        if (pe == null) return base;
        if (pe.isBurning())
            base += this.getFloatOrDefault("fire", 0F);
        if (pe.isInWater())//does not apply twice
            base += this.getFloatOrDefault("water", 0F);
        else if (pe.isPotionActive(FHMobEffects.WET.get())) {
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
	@Override
	public String toString() {
		return "ArmorTempData [insulation=" + insulation + ", heat_proof=" + heat_proof + ", wind_proof=" + wind_proof + "]";
	}
}

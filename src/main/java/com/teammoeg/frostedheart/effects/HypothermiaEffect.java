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

package com.teammoeg.frostedheart.effects;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.FHDamageSources;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

public class HypothermiaEffect extends Effect {
    public HypothermiaEffect(EffectType typeIn, int liquidColorIn) {
        super(typeIn, liquidColorIn);
    }

    @Override
    public void performEffect(LivingEntity entityLivingBaseIn, int amplifier) {
        if (entityLivingBaseIn instanceof ServerPlayerEntity) {
        	((ServerPlayerEntity) entityLivingBaseIn).addExhaustion(amplifier<2?0.044f*(amplifier+1):0.132f);
        	if(amplifier>1) {
	            if (entityLivingBaseIn.getHealth() > 20.0F) {
	                entityLivingBaseIn.attackEntityFrom(FHDamageSources.HYPOTHERMIA, 1F);
	            } else if (entityLivingBaseIn.getHealth() > 10.0F) {
	                entityLivingBaseIn.attackEntityFrom(FHDamageSources.HYPOTHERMIA, 0.5F);
	            } else if (entityLivingBaseIn.getHealth() > 5.0F) {
	                entityLivingBaseIn.attackEntityFrom(FHDamageSources.HYPOTHERMIA, 0.3F);
	            } else {
	                entityLivingBaseIn.attackEntityFrom(FHDamageSources.HYPOTHERMIA, 0.2F);
	            }
        	}
        }
    }

    public boolean isReady(int duration, int amplifier) {

        int k = 60 >> Math.max(amplifier - 2,0);
        if (k > 1) {
            return duration % k == 0;
        }
        return true;
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return ImmutableList.of();
    }
}

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

package com.teammoeg.frostedheart.effects;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.FHDamageSources;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

public class HyperthermiaEffect extends Effect {
    public HyperthermiaEffect(EffectType typeIn, int liquidColorIn) {
        super(typeIn, liquidColorIn);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return ImmutableList.of();
    }

    public boolean isDurationEffectTick(int duration, int amplifier) {
        if (amplifier <= 1) return false;//0 or 1 does not damage
        int k = 60 >> (amplifier - 2);//2 or higher does damage
        if (k > 0) {
            return duration % k == 0;
        }
        return true;
    }

    @Override
    public void applyEffectTick(LivingEntity entityLivingBaseIn, int amplifier) {
        if (entityLivingBaseIn instanceof ServerPlayerEntity) {
            if (entityLivingBaseIn.getHealth() > 20.0F) {
                entityLivingBaseIn.hurt(FHDamageSources.HYPERTHERMIA, 1F);
            } else if (entityLivingBaseIn.getHealth() > 10.0F) {
                entityLivingBaseIn.hurt(FHDamageSources.HYPERTHERMIA, 0.5F);
            } else if (entityLivingBaseIn.getHealth() > 5.0F) {
                entityLivingBaseIn.hurt(FHDamageSources.HYPERTHERMIA, 0.3F);
            } else {
                entityLivingBaseIn.hurt(FHDamageSources.HYPERTHERMIA, 0.2F);
            }
        }
    }
}

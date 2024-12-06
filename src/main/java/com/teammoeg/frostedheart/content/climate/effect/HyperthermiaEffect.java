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

package com.teammoeg.frostedheart.content.climate.effect;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.FHDamageTypes;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class HyperthermiaEffect extends MobEffect {
    public HyperthermiaEffect(MobEffectCategory typeIn, int liquidColorIn) {
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
        if (entityLivingBaseIn instanceof ServerPlayer) {
            if (entityLivingBaseIn.getHealth() > 20.0F) {
                entityLivingBaseIn.hurt(FHDamageTypes.createSource(entityLivingBaseIn.level(), FHDamageTypes.HYPERTHERMIA, entityLivingBaseIn) , 1F);
            } else if (entityLivingBaseIn.getHealth() > 10.0F) {
                entityLivingBaseIn.hurt(FHDamageTypes.createSource(entityLivingBaseIn.level(), FHDamageTypes.HYPERTHERMIA, entityLivingBaseIn) , 0.5F);
            } else if (entityLivingBaseIn.getHealth() > 5.0F) {
                entityLivingBaseIn.hurt(FHDamageTypes.createSource(entityLivingBaseIn.level(), FHDamageTypes.HYPERTHERMIA, entityLivingBaseIn) , 0.3F);
            } else {
                entityLivingBaseIn.hurt(FHDamageTypes.createSource(entityLivingBaseIn.level(), FHDamageTypes.HYPERTHERMIA, entityLivingBaseIn) , 0.2F);
            }
        }
    }
}

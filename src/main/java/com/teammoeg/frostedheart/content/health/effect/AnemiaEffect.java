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

package com.teammoeg.frostedheart.content.health.effect;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.teammoeg.chorda.util.CUtils;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;

public class AnemiaEffect extends MobEffect {

    public AnemiaEffect(MobEffectCategory typeIn, int liquidColorIn) {
        super(typeIn, liquidColorIn);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return ImmutableList.of();
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 100 == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity entityLivingBaseIn, int amplifier) {
        entityLivingBaseIn.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, amplifier)));
        entityLivingBaseIn.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, amplifier)));
        entityLivingBaseIn.addEffect(CUtils.noHeal(new MobEffectInstance(MobEffects.WEAKNESS, 100, amplifier * 2)));
        //entityLivingBaseIn.addEffect(CUtils.noHeal(new MobEffectInstance(EffectRegistry.THIRST, 100, amplifier * 2)));
    }
}

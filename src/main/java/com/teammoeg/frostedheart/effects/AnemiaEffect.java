/*
 * Copyright (c) 2022-2024 TeamMoeg
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
import com.teammoeg.frostedheart.util.FHUtils;

import gloridifice.watersource.registry.EffectRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.Effects;

public class AnemiaEffect extends Effect {

    public AnemiaEffect(EffectType typeIn, int liquidColorIn) {
        super(typeIn, liquidColorIn);
    }

    @Override
    public void performEffect(LivingEntity entityLivingBaseIn, int amplifier) {
        entityLivingBaseIn.addPotionEffect(FHUtils.noHeal(new EffectInstance(Effects.SLOWNESS, 100, amplifier)));
        entityLivingBaseIn.addPotionEffect(FHUtils.noHeal(new EffectInstance(Effects.MINING_FATIGUE, 100, amplifier)));
        entityLivingBaseIn.addPotionEffect(FHUtils.noHeal(new EffectInstance(Effects.WEAKNESS, 100, amplifier * 2)));
        entityLivingBaseIn.addPotionEffect(FHUtils.noHeal(new EffectInstance(EffectRegistry.THIRST, 100, amplifier * 2)));
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return duration % 100 == 0;
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return ImmutableList.of();
    }
}

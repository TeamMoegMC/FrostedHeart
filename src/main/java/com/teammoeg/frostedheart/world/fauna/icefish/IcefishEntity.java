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

package com.teammoeg.frostedheart.world.fauna.icefish;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.fish.AbstractGroupFishEntity;
import net.minecraft.entity.passive.fish.SalmonEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;

public class IcefishEntity extends AbstractGroupFishEntity {
    public IcefishEntity(EntityType<? extends SalmonEntity> p_i50246_1_, World p_i50246_2_) {
        super(p_i50246_1_, p_i50246_2_);
    }

    public int getMaxGroupSize() {
        return 5;
    }

    protected ItemStack getFishBucket() {
        return new ItemStack(Items.SALMON_BUCKET);
    }

    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_SALMON_AMBIENT;
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_SALMON_DEATH;
    }

    protected SoundEvent getHurtSound(DamageSource p_184601_1_) {
        return SoundEvents.ENTITY_SALMON_HURT;
    }

    protected SoundEvent getFlopSound() {
        return SoundEvents.ENTITY_SALMON_FLOP;
    }
}

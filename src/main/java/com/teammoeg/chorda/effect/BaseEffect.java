/*
 * Copyright (c) 2026 TeamMoeg
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

package com.teammoeg.chorda.effect;

import java.util.List;

import com.google.common.collect.ImmutableList;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 基础效果类。提供一个默认无操作的MobEffect实现，
 * 不会被牛奶等物品治愈，且默认不触发持续时间效果刻。
 * <p>
 * Base effect class. Provides a default no-op MobEffect implementation
 * that cannot be cured by milk or similar items, and does not trigger
 * duration effect ticks by default.
 */
public class BaseEffect extends MobEffect {

    /**
     * 创建基础效果实例。
     * <p>
     * Creates a base effect instance.
     *
     * @param typeIn 效果类别（有益/有害/中性） / The effect category (beneficial/harmful/neutral)
     * @param liquidColorIn 药水粒子的颜色值 / The color value for potion particles
     */
    public BaseEffect(MobEffectCategory typeIn, int liquidColorIn) {
        super(typeIn, liquidColorIn);
    }

    /**
     * 应用瞬时效果。默认不执行任何操作。
     * <p>
     * Applies an instantaneous effect. Does nothing by default.
     *
     * @param source 效果的直接来源实体 / The direct source entity
     * @param indirectSource 效果的间接来源实体 / The indirect source entity
     * @param entityLivingBaseIn 受影响的生物实体 / The affected living entity
     * @param amplifier 效果等级 / The effect amplifier
     * @param health 健康值参数 / The health parameter
     */
    @Override
    public void applyInstantenousEffect(Entity source, Entity indirectSource, LivingEntity entityLivingBaseIn, int amplifier,
                             double health) {
    }

    /**
     * 获取可以治愈此效果的物品列表。返回空列表，表示此效果不可被治愈。
     * <p>
     * Gets the list of items that can cure this effect. Returns an empty list,
     * indicating this effect cannot be cured.
     *
     * @return 空的不可变列表 / An empty immutable list
     */
    @Override
    public List<ItemStack> getCurativeItems() {
        return ImmutableList.of();
    }

    /**
     * 检查当前持续时间是否应触发效果刻。默认返回false，表示不触发。
     * <p>
     * Checks whether the current duration should trigger an effect tick.
     * Returns false by default, meaning no ticks are triggered.
     *
     * @param duration 剩余持续时间 / The remaining duration
     * @param amplifier 效果等级 / The effect amplifier
     * @return 始终返回false / Always returns false
     */
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return false;
    }

    /**
     * 在效果刻时应用效果。默认不执行任何操作。
     * <p>
     * Applies the effect on an effect tick. Does nothing by default.
     *
     * @param entityLivingBaseIn 受影响的生物实体 / The affected living entity
     * @param amplifier 效果等级 / The effect amplifier
     */
    @Override
    public void applyEffectTick(LivingEntity entityLivingBaseIn, int amplifier) {
    }
}

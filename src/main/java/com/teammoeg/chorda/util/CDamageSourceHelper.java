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

package com.teammoeg.chorda.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
/**
 * 伤害来源构造辅助类，提供便捷方法来创建不同类型的伤害来源。
 * 支持指定攻击者、目标实体、攻击位置等多种场景。
 * <p>
 * Damage source construction helper class providing convenience methods
 * for creating various types of damage sources. Supports specifying attacker,
 * target entity, attack position, and other scenarios.
 */
public class CDamageSourceHelper {

	private CDamageSourceHelper() {
	}

	/**
	 * 完整构造伤害来源，用于覆盖攻击位置，例如实体从另一侧发起攻击。
	 * 位置变化会影响格挡和击退方向。
	 * <p>
	 * Fully construct a damage source with custom position, used when overwriting
	 * the attacking position. Changes in position affect blocking and knockback.
	 *
	 * @param level 世界实例 / the world instance
	 * @param type 伤害类型 / the damage type
	 * @param source 伤害来源实体 / the source entity
	 * @param dest 被攻击实体 / the target entity
	 * @param pos 攻击位置 / the attack position
	 * @return 伤害来源 / the damage source
	 */
	public static DamageSource source(Level level, ResourceKey<DamageType> type, Entity source, Entity dest,Vec3 pos) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),dest,source,pos);
	}

	/**
	 * 构造攻击伤害来源，来源和目标实体不同，使用来源实体位置。
	 * <p>
	 * Construct an attack damage source where source and target entities differ,
	 * using the source entity's position.
	 *
	 * @param level 世界实例 / the world instance
	 * @param type 伤害类型 / the damage type
	 * @param source 攻击者 / the attacker
	 * @param dest 被攻击者 / the target
	 * @return 伤害来源 / the damage source
	 */
	public static DamageSource source(Level level, ResourceKey<DamageType> type, Entity source, Entity dest) {
		return source(level,type,source,dest,null);
	}

	/**
	 * 构造自我伤害来源，来源和目标是同一实体，如中毒。
	 * <p>
	 * Construct a self-inflicted damage source where source and target are the same entity,
	 * such as from poison.
	 *
	 * @param level 世界实例 / the world instance
	 * @param type 伤害类型 / the damage type
	 * @param dest 受伤实体 / the damaged entity
	 * @return 伤害来源 / the damage source
	 */
	public static DamageSource source(Level level, ResourceKey<DamageType> type, Entity dest) {
		return source(level,type,dest,dest);
	}

	/**
	 * 构造环境伤害来源（来自方块），无击退效果。
	 * <p>
	 * Construct an environmental damage source from a block, with no knockback.
	 *
	 * @param level 世界实例 / the world instance
	 * @param type 伤害类型 / the damage type
	 * @param pos 伤害来源位置 / the damage source position
	 * @return 伤害来源 / the damage source
	 */
	public static DamageSource source(Level level, ResourceKey<DamageType> type, Vec3 pos) {
		return source(level,type,null,null,pos);
	}

	/**
	 * 构造环境伤害来源（来自环境），无击退效果，无特定位置。
	 * <p>
	 * Construct an environmental damage source from the environment, with no knockback
	 * and no specific position.
	 *
	 * @param level 世界实例 / the world instance
	 * @param type 伤害类型 / the damage type
	 * @return 伤害来源 / the damage source
	 */
	public static DamageSource source(Level level, ResourceKey<DamageType> type) {
	    return source(level,type,(Vec3)null);
	}

}

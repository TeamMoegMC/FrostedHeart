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

package com.teammoeg.chorda.mixin;

import java.util.IdentityHashMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;

/**
 * Forge能力管理器的Mixin访问器接口，用于访问内部的providers映射。
 * <p>
 * Mixin accessor interface for Forge's CapabilityManager, providing access to the internal providers map.
 */
@Mixin(CapabilityManager.class)
public interface CapabilityManagerAccess {
	/**
	 * 获取能力管理器内部的提供者映射表。
	 * <p>
	 * Gets the internal providers map from the capability manager.
	 *
	 * @return 能力名称到能力实例的映射 / The map from capability names to capability instances
	 */
	@Accessor(value="providers", remap=false)
	IdentityHashMap<String, Capability<?>> getProviders();
}

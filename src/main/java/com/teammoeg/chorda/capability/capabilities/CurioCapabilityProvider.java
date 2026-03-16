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

package com.teammoeg.chorda.capability.capabilities;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.util.NonNullSupplier;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICurio;

/**
 * Curios模组集成的能力提供者。
 * 为物品提供{@link ICurio}能力，使其可以作为Curios饰品槽中的物品使用。
 * <p>
 * Capability provider for Curios mod integration.
 * Provides the {@link ICurio} capability to items, allowing them to be used
 * as items in Curios accessory slots.
 */
public class CurioCapabilityProvider implements ICapabilityProvider{
	/** ICurio能力的延迟加载可选值 / Lazily loaded optional value of the ICurio capability */
	LazyOptional<ICurio> lazyCap;

	/**
	 * 使用指定的ICurio供应器构造提供者。
	 * <p>
	 * Constructs a provider with the specified ICurio supplier.
	 *
	 * @param lazyCap ICurio实例的供应器 / The supplier of the ICurio instance
	 */
	public CurioCapabilityProvider(NonNullSupplier<ICurio> lazyCap) {
		super();
		this.lazyCap = LazyOptional.of(lazyCap);
	}

	/**
	 * 获取请求的能力。仅当请求的是{@link CuriosCapability#ITEM}时返回ICurio实例。
	 * <p>
	 * Gets the requested capability. Only returns the ICurio instance when
	 * {@link CuriosCapability#ITEM} is requested.
	 *
	 * @param cap 请求的能力 / The requested capability
	 * @param side 访问方向 / The access direction
	 * @param <T> 能力类型 / The capability type
	 * @return 包含ICurio实例的LazyOptional，或空 / A LazyOptional containing the ICurio instance, or empty
	 */
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
		if(cap==CuriosCapability.ITEM) {
			return lazyCap.cast();
		}
		return LazyOptional.empty();
	}

}

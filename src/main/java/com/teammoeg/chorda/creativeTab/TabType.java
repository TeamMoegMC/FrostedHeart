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

package com.teammoeg.chorda.creativeTab;

import java.util.function.Predicate;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.RegistryObject;
/**
 * 特定创造模式标签页的符号表示。
 * 用于匹配和标识创造模式标签页，支持通过谓词或注册对象进行匹配。
 * <p>
 * Symbolic representative for a specific creative tab.
 * Used to match and identify creative mode tabs, supporting matching via predicates or registry objects.
 */
public class TabType implements Predicate<ResourceKey<CreativeModeTab>>{
	/** 隐藏标签类型，永远不匹配任何标签页 / Hidden tab type that never matches any tab */
	public static final TabType HIDDEN=new TabType(e->false);
	/** 用于匹配标签页的谓词 / The predicate used to match tabs */
	private final Predicate<ResourceKey<CreativeModeTab>> predicate;

	/**
	 * 通过谓词创建标签类型。
	 * <p>
	 * Creates a tab type with a predicate.
	 *
	 * @param predicate 用于匹配标签页资源键的谓词 / The predicate for matching tab resource keys
	 */
	public TabType(Predicate<ResourceKey<CreativeModeTab>> predicate) {
		this.predicate = predicate;
	}
	/**
	 * 通过注册对象创建标签类型。匹配与注册对象键相等的标签页。
	 * <p>
	 * Creates a tab type from a registry object. Matches tabs whose key equals the registry object's key.
	 *
	 * @param predicate 创造模式标签页的注册对象 / The registry object of the creative mode tab
	 */
	public TabType(RegistryObject<CreativeModeTab> predicate) {
		this.predicate = e->e.equals(predicate.getKey());
	}

	/**
	 * 测试给定的创造模式标签页资源键是否匹配此标签类型。
	 * <p>
	 * Tests whether the given creative mode tab resource key matches this tab type.
	 *
	 * @param t 要测试的创造模式标签页资源键 / The creative mode tab resource key to test
	 * @return 如果匹配则为true / true if it matches
	 */
	@Override
	public boolean test(ResourceKey<CreativeModeTab> t) {
		return predicate.test(t);
	}
	
}

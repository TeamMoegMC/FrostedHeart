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

package com.teammoeg.chorda.multiblock.components;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

/**
 * 基于 Codec 的多方块状态工厂，用于创建 {@link CCodecState} 实例。
 * 封装了保存 Codec、同步 Codec 和数据对象的工厂方法，以便在多方块初始化时
 * 方便地创建带有默认值的状态实例。
 * <p>
 * A Codec-based multiblock state factory used to create {@link CCodecState} instances.
 * Encapsulates the save Codec, sync Codec, and a factory method for data objects, making it
 * convenient to create state instances with default values during multiblock initialization.
 *
 * @param <T> 状态数据类型 / The state data type
 * @see CCodecState
 */
public class CCodecStateFactory<T> {

	/** 用于存档保存的 Codec / Codec used for save persistence */
	final Codec<T> Save;

	/** 用于客户端同步的 Codec / Codec used for client synchronization */
	final Codec<T> Sync;

	/** 用于创建默认数据实例的工厂方法 / Factory method for creating default data instances */
	final Supplier<T> factory;

	/**
	 * 使用保存 Codec、同步 Codec 和工厂方法构造状态工厂。
	 * <p>
	 * Constructs a state factory with save Codec, sync Codec, and a factory method.
	 *
	 * @param save 用于存档保存序列化的 Codec / The Codec for save persistence serialization
	 * @param sync 用于客户端同步序列化的 Codec / The Codec for client synchronization serialization
	 * @param factory 用于创建默认数据实例的供应器 / The supplier for creating default data instances
	 */
	public CCodecStateFactory(Codec<T> save, Codec<T> sync, Supplier<T> factory) {
		super();
		Save = save;
		Sync = sync;
		this.factory = factory;
	}

}

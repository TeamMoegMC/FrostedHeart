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

package com.teammoeg.chorda.multiblock.components;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

public class CCodecStateFactory<T> {
	final Codec<T> Save;
	final Codec<T> Sync;
	final Supplier<T> factory;
	public CCodecStateFactory(Codec<T> save, Codec<T> sync, Supplier<T> factory) {
		super();
		Save = save;
		Sync = sync;
		this.factory = factory;
	}

}

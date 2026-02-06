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

package com.teammoeg.chorda.config;

import java.io.File;

import com.mojang.serialization.Codec;

import net.minecraftforge.fml.loading.FMLPaths;
/**
 * Represent a subfolder with handful of configuration files
 * */
public record ConfigFileType<T>(Codec<T> codec,File folder) {
	public ConfigFileType(Codec<T> codec,String subfolder) {
		this(codec,new File(FMLPaths.CONFIGDIR.get().toFile(),subfolder));
	}

	@Override
	public String toString() {
		return folder.getName() ;
	}
}

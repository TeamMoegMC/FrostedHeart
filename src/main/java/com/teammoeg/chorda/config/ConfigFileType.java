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
 * 表示一个包含若干配置文件的子文件夹。
 * 使用Codec进行配置数据的序列化和反序列化。
 * <p>
 * Represents a subfolder containing a handful of configuration files.
 * Uses a Codec for serialization and deserialization of configuration data.
 *
 * @param codec 用于序列化/反序列化的编解码器 / The codec for serialization/deserialization
 * @param folder 配置文件所在的文件夹 / The folder where configuration files are located
 * @param <T> 配置数据类型 / The configuration data type
 */
public record ConfigFileType<T>(Codec<T> codec,File folder) {
	/**
	 * 通过子文件夹名称创建配置文件类型。文件夹路径基于Forge配置目录。
	 * <p>
	 * Creates a config file type by subfolder name. The folder path is based on the Forge config directory.
	 *
	 * @param codec 用于序列化/反序列化的编解码器 / The codec for serialization/deserialization
	 * @param subfolder 配置目录下的子文件夹名称 / The subfolder name under the config directory
	 */
	public ConfigFileType(Codec<T> codec,String subfolder) {
		this(codec,new File(FMLPaths.CONFIGDIR.get().toFile(),subfolder));
	}

	/**
	 * 返回配置文件夹的名称作为字符串表示。
	 * <p>
	 * Returns the config folder name as the string representation.
	 *
	 * @return 文件夹名称 / The folder name
	 */
	@Override
	public String toString() {
		return folder.getName() ;
	}
}

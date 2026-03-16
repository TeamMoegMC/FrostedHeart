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

package com.teammoeg.chorda.io;

import com.google.gson.JsonElement;

/**
 * 可写入接口，扩展PacketWritable，同时支持JSON序列化。名称非拼写错误，是为了避免命名冲突。
 * <p>
 * Writable interface extending PacketWritable with JSON serialization support. The name is intentional to avoid naming conflicts.
 */
public interface Writeable extends PacketWritable {
	/**
	 * 将此对象序列化为JSON元素。
	 * <p>
	 * Serializes this object to a JSON element.
	 *
	 * @return JSON元素 / the JSON element
	 * @deprecated 不应调用此方法进行序列化 / should not call this method to serialize things
	 */
	@Deprecated JsonElement serialize();
	
}

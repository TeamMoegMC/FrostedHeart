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

/**
 * 反射序列化器（Marshaller）包。提供基于Java反射的自动NBT序列化框架，
 * 通过分析类的字段信息自动完成对象与NBT之间的转换，支持嵌套对象和列表。
 * <p>
 * Reflection marshaller package. Provides an automatic NBT serialization framework
 * based on Java reflection, automatically converting between objects and NBT by
 * analyzing class field information, with support for nested objects and lists.
 *
 * @see com.teammoeg.chorda.io.marshaller.Marshaller
 * @see com.teammoeg.chorda.io.marshaller.ClassInfo
 * @see com.teammoeg.chorda.io.marshaller.ReflectionCodec
 */
package com.teammoeg.chorda.io.marshaller;

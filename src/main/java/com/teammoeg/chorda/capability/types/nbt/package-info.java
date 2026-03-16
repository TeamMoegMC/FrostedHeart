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
 * 基于NBT的能力序列化包。使用Minecraft的NBT系统进行能力数据的序列化和反序列化，
 * 采用就地更新语义（反序列化时更新现有实例，而非创建新实例）。
 * <p>
 * NBT-based capability serialization package. Uses Minecraft's NBT system for
 * capability data serialization and deserialization, with in-place update semantics
 * (existing instances are updated during deserialization, not replaced with new ones).
 *
 * @see com.teammoeg.chorda.capability.types.nbt.NBTCapabilityType
 * @see com.teammoeg.chorda.capability.types.nbt.NBTCapabilityProvider
 */
package com.teammoeg.chorda.capability.types.nbt;

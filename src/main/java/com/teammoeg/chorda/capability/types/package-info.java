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
 * 能力类型定义包。定义能力类型的基础接口，并通过子包提供三种序列化策略：
 * Codec序列化、NBT序列化和非持久化（瞬态）。
 * <p>
 * Capability type definition package. Defines the base interface for capability
 * types, with three serialization strategies provided by sub-packages:
 * Codec-based, NBT-based, and non-persistent (transient).
 *
 * @see com.teammoeg.chorda.capability.types.CapabilityType
 * @see com.teammoeg.chorda.capability.types.codec
 * @see com.teammoeg.chorda.capability.types.nbt
 * @see com.teammoeg.chorda.capability.types.nonpresistent
 */
package com.teammoeg.chorda.capability.types;

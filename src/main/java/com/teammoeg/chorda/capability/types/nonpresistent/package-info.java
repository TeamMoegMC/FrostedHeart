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
 * 瞬态（非持久化）能力包。提供仅存在于运行时的能力实现，
 * 不进行任何序列化，适用于缓存或临时运行时状态。
 * <p>
 * Transient (non-persistent) capability package. Provides capability implementations
 * that exist only at runtime without any serialization, suitable for caching or
 * temporary runtime state.
 *
 * @see com.teammoeg.chorda.capability.types.nonpresistent.TransientCapability
 * @see com.teammoeg.chorda.capability.types.nonpresistent.TransientCapabilityProvider
 */
package com.teammoeg.chorda.capability.types.nonpresistent;

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
 * 序列化器注册表包。提供多态对象的类型注册和序列化注册表框架，
 * 支持JSON、NBT、网络数据包和Codec等多种序列化后端，
 * 通过类型标识符实现自动多态分发。
 * <p>
 * Serializer registry package. Provides type registration and serializer registry
 * frameworks for polymorphic objects, supporting JSON, NBT, network packets, and
 * Codec as serialization backends, with automatic polymorphic dispatch via type identifiers.
 *
 * @see com.teammoeg.chorda.io.registry.SerializerRegistry
 * @see com.teammoeg.chorda.io.registry.TypeRegistry
 * @see com.teammoeg.chorda.io.registry.TypedCodecRegistry
 * @see com.teammoeg.chorda.io.registry.PacketBufferSerializerRegistry
 */
package com.teammoeg.chorda.io.registry;

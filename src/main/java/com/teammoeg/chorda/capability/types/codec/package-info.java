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
 * 基于Codec的能力序列化包。使用Mojang的{@link com.mojang.serialization.Codec}系统
 * 进行能力数据的序列化和反序列化，采用对象替换语义（反序列化时创建新实例）。
 * <p>
 * Codec-based capability serialization package. Uses Mojang's
 * {@link com.mojang.serialization.Codec} system for capability data serialization
 * and deserialization, with object replacement semantics (new instances created on deserialization).
 *
 * @see com.teammoeg.chorda.capability.types.codec.CodecCapabilityType
 * @see com.teammoeg.chorda.capability.types.codec.CodecCapabilityProvider
 */
package com.teammoeg.chorda.capability.types.codec;

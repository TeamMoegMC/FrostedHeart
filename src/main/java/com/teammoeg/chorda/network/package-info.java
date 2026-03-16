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
 * 网络通信包。封装Forge的SimpleChannel网络系统，提供消息基类和
 * 基于ASM的高性能反序列化，支持容器数据同步和NBT消息传输。
 * <p>
 * Network communication package. Wraps Forge's SimpleChannel network system,
 * providing message base classes and ASM-based high-performance deserialization,
 * with support for container data sync and NBT message transmission.
 *
 * @see com.teammoeg.chorda.network.CBaseNetwork
 * @see com.teammoeg.chorda.network.CMessage
 */
package com.teammoeg.chorda.network;

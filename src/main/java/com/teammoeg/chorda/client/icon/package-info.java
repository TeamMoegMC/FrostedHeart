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
 * 图标系统包。提供统一的图标抽象和扁平化图标(FlatIcon)渲染系统，
 * 支持通过图标名称查找和渲染矢量风格图标。{@link com.teammoeg.chorda.client.icon.CIconBatch}
 * 为大量虚拟节点提供有序物品图标批次；不支持批次的图标会成为立即绘制屏障。
 * <p>
 * Icon system package. Provides a unified icon abstraction and flat icon (FlatIcon)
 * rendering system, supporting lookup and rendering of vector-style icons by name.
 * {@link com.teammoeg.chorda.client.icon.CIconBatch} provides ordered item-icon passes
 * for large virtual canvases, with immediate barriers for unsupported icon types.
 */
package com.teammoeg.chorda.client.icon;

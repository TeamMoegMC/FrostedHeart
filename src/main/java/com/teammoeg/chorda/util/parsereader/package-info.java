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
 * 解析读取器包。提供逐行、逐字符的文本解析框架，
 * 支持多种输入源（字符串、流、Reader等），带有行号和位置追踪。
 * <p>
 * Parse reader package. Provides a line-by-line, character-by-character text
 * parsing framework, supporting multiple input sources (strings, streams, Readers, etc.),
 * with line number and position tracking.
 *
 * @see com.teammoeg.chorda.util.parsereader.ParseReader
 * @see com.teammoeg.chorda.util.parsereader.source
 */
package com.teammoeg.chorda.util.parsereader;

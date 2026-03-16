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

package com.teammoeg.chorda.util;

/**
 * 文本进度条渲染辅助类，提供使用Unicode字符创建文本进度条的方法。
 * 支持正向、反向、偏移和区间模式。
 * <p>
 * Text progress bar rendering helper class providing methods to create text progress bars
 * using Unicode characters. Supports forward, reversed, offset, and interval modes.
 */
public class TextProgressBarHelper {
    /**
     * 创建从左到右填充的进度条。
     * <p>
     * Create a progress bar that fills from left to right.
     *
     * @param length 进度条总长度 / the total length of the progress bar
     * @param filledLength 已填充长度 / the filled length
     * @return 进度条字符串 / the progress bar string
     */
    public static String makeProgressBar(int length, int filledLength) {
        String bar = " ";
        int emptySpaces = length - filledLength;
        for (int i = 0; i < filledLength; i++)
            bar += "\u2588";
        for (int i = 0; i < emptySpaces; i++)
            bar += "\u2592";
        return bar;
    }

    /**
     * 创建从右到左填充的反向进度条。
     * <p>
     * Create a reversed progress bar that fills from right to left.
     *
     * @param length 进度条总长度 / the total length of the progress bar
     * @param filledLength 已填充长度 / the filled length
     * @return 进度条字符串 / the progress bar string
     */
    public static String makeProgressBarReversed(int length, int filledLength) {
        String bar = " ";
        int emptySpaces = length - filledLength;
        for (int i = 0; i < emptySpaces; i++)
            bar += "\u2592";
        for (int i = 0; i < filledLength; i++)
            bar += "\u2588";
        return bar;
    }

    /**
     * 创建带起始偏移的进度条，偏移量左侧不填充。
     * <p>
     * Create a progress bar with a starting offset, where the area left of the offset is not filled.
     *
     * @param length 进度条总长度 / the total length of the progress bar
     * @param filledLength 已填充长度 / the filled length
     * @param offset 起始偏移量 / the starting offset
     * @return 进度条字符串 / the progress bar string
     */
    public static String makeProgressBarOffset(int length, int filledLength, int offset) {
        String bar = "";
        int emptySpaces = length - filledLength - offset;
        for (int i = 0; i < offset; i++)
            bar += "\u2592";
        for (int i = 0; i < filledLength; i++)
            bar += "\u2588";
        for (int i = 0; i < emptySpaces; i++)
            bar += "\u2592";
        return bar;
    }

    /**
     * 创建区间进度条，填充指定区间[min, max]范围内的部分。
     * <p>
     * Create an interval progress bar, filling the portion within the specified [min, max] range.
     *
     * @param length 进度条总长度，应大于max / the total length, should be greater than max
     * @param min 区间下界 / the lower bound of the interval
     * @param max 区间上界 / the upper bound of the interval
     * @return 进度条字符串 / the progress bar string
     */
    public static String makeProgressBarInterval(int length, int min, int max) {
        return makeProgressBarOffset(length, max - min, min);
    }
}

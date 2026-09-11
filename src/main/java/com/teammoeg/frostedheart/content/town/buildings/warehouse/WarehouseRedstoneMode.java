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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

/**
 * 城镇仓库自动化设备共用的红石模式。
 * <p>
 * 仓库发信器使用 {@link #HIGH_SIGNAL} / {@link #LOW_SIGNAL} 决定何时发出红石信号
 * （分别对应"存量 ≥ 阈值"与"存量 < 阈值"，与 AE2 发信器一致）；
 * 仓库接口使用全部三个值决定何时允许从城镇仓库补货输出。
 * <p>
 * Shared redstone mode for town-owned warehouse automation devices.
 * <p>
 * The warehouse level emitter uses {@link #HIGH_SIGNAL} / {@link #LOW_SIGNAL} to decide when to
 * emit a redstone signal (stock at or above threshold / stock below threshold, mirroring the AE2
 * level emitter). The warehouse interface uses all three values to decide when restocking output
 * from the town warehouse is allowed.
 */
public enum WarehouseRedstoneMode {
    /**
     * 忽略红石信号，始终允许输出。仅用于仓库接口。
     * <p>
     * Ignore redstone signals; output is always allowed. Only used by the warehouse interface.
     */
    IGNORE,
    /**
     * 接口：有红石信号时才输出。发信器：存量大于等于阈值时发出信号。
     * <p>
     * Interface: output only while powered. Emitter: emit while stock is at or above the threshold.
     */
    HIGH_SIGNAL,
    /**
     * 接口：无红石信号时才输出。发信器：存量低于阈值时发出信号。
     * <p>
     * Interface: output only while unpowered. Emitter: emit while stock is below the threshold.
     */
    LOW_SIGNAL;

    /**
     * 按当前红石模式判断接口是否允许补货输出。
     * <p>
     * Checks whether restocking output is allowed under this mode.
     *
     * @param powered 方块当前是否被红石充能 / whether the block is currently powered
     * @return 允许输出时为 true / true when output is allowed
     */
    public boolean allowsOutput(boolean powered) {
        return switch (this) {
            case IGNORE -> true;
            case HIGH_SIGNAL -> powered;
            case LOW_SIGNAL -> !powered;
        };
    }

    /**
     * 循环到下一个接口红石控制模式（IGNORE → HIGH_SIGNAL → LOW_SIGNAL）。
     * <p>
     * Cycles to the next interface redstone control mode.
     *
     * @return 下一个模式 / the next mode
     */
    public WarehouseRedstoneMode nextControlMode() {
        WarehouseRedstoneMode[] modes = values();
        return modes[(ordinal() + 1) % modes.length];
    }

    /**
     * 切换到另一个发信器模式（仅 HIGH_SIGNAL 与 LOW_SIGNAL 互换）。
     * <p>
     * Toggles between the two emitter modes.
     *
     * @return 另一个发信器模式 / the other emitter mode
     */
    public WarehouseRedstoneMode nextEmitterMode() {
        return this == LOW_SIGNAL ? HIGH_SIGNAL : LOW_SIGNAL;
    }

    /**
     * 以序号读取模式，序号越界时回退到默认值。
     * <p>
     * Reads a mode by ordinal, falling back to the given default when out of bounds.
     *
     * @param ordinal 模式序号 / the mode ordinal
     * @param fallback 序号非法时的默认值 / fallback mode for invalid ordinals
     * @return 对应模式 / the resolved mode
     */
    public static WarehouseRedstoneMode byOrdinal(int ordinal, WarehouseRedstoneMode fallback) {
        WarehouseRedstoneMode[] modes = values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : fallback;
    }
}

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

package com.teammoeg.frostedheart.content.world.entities;

/**
 * 匍匐集群（雪原深处的好奇心）的阶段枚举。
 * <p>
 * Phase enumeration of the crawling nanite cluster ("Curiosity of the Deep
 * Frostland") boss. Transitions are driven by the server-side state machine in
 * {@link CuriosityEntity}.
 */
public enum CuriosityPhase {
    /** 匍匐休眠 / Dormant, lurking below the surface. */
    DORMANT,
    /** 苏醒演出 / Rising, the arena wakes up. */
    RISING,
    /** 地下追踪 / Underground tracking assault. */
    HUNT,
    /** 迷宫升起 / Snow wall maze rising. */
    MAZE,
    /** 核心露出 / Vulnerable core exposed. */
    EXPOSED,
    /** 钻回地下 / Burrowing back underground for the next round. */
    BURROW,
    /** 被火驱散 / Dispersed by fire, the fight is won. */
    DISPERSED;

    /** 是否处于战斗状态（Boss 条与音乐可见） / Whether this is a combat phase. */
    public boolean isCombat() {
        return this != DORMANT && this != DISPERSED;
    }

    /** 冷场是否应挂载 / Whether the cold field should be active. */
    public boolean isColdActive() {
        return this == RISING || this == HUNT || this == MAZE || this == EXPOSED || this == BURROW;
    }
}

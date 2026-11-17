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

package com.teammoeg.frostedheart.content.town.client.board;

import net.minecraft.Util;
import net.minecraft.util.Mth;

/**
 * 木牌抽出：换页时选中的木牌约 0.15 秒内滑出来，刚离开的那块滑回去。木牌每次重建都是新的，所以时间记在界面上。
 * <p>
 * Plaques pulled out: on a page change the chosen plaque slides out over about 0.15 seconds and the one just left
 * slides back. Plaques are rebuilt with the page, so the timing lives with the screen.
 */
public final class TabSlide {
    private static final float MILLIS = 150f;
    private int previous = -1;
    private long switchedAt = -1;

    /** Call when the page changes, with the index of the page being left. */
    public void leave(int from) {
        previous = from;
        switchedAt = Util.getMillis();
    }

    /** How far the plaque at {@code index} sticks out now, from 0 (in line) to 1 (pulled out). */
    public float out(int index, int active) {
        float t = switchedAt < 0 ? 1 : Mth.clamp((Util.getMillis() - switchedAt) / MILLIS, 0f, 1f);
        float eased = 1 - (1 - t) * (1 - t);
        if (index == active) return index == previous ? 1 : eased;
        return index == previous ? 1 - eased : 0;
    }
}

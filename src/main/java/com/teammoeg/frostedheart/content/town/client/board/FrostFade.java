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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.Util;
import net.minecraft.util.Mth;

/**
 * 霜的过渡：每块布告板记住玩家上次看到的霜冻等级，打开后约 0.6 秒内从那一级过渡到现在这一级；看着的时候等级变了也一样。
 * 变冷时霜慢慢爬上纸面，冰凌往下长；变暖时霜化开，冰凌缩回去。只是表现，能否工作仍按原始值判断。
 * <p>
 * The frost transition: each board remembers the frost level the player last saw and, once opened, eases from it to the
 * current level over about 0.6 seconds; a change while the board is open eases the same way. Getting colder, frost
 * creeps over the paper and icicles grow; getting warmer, it melts and the icicles draw back. Presentation only; whether
 * a building works still follows the raw values.
 */
public final class FrostFade {
    private static final float MILLIS = 600f;
    /** The level last shown per board, for the next time it opens. A town has few boards, so this stays small. */
    private static final Map<Object, Integer> SEEN = new HashMap<>();
    @Nullable
    private static FrostFade drawing;

    private final Object key;
    private long startedAt = -1;
    private float from;
    private int level;

    /** @param key what identifies the board between openings, e.g. the building's position */
    public FrostFade(Object key) {
        this.key = key;
    }

    /** Records the board's own frost level. The first call starts from what was seen last; a later change starts anew. */
    public void track(int level) {
        long now = Util.getMillis();
        if (startedAt < 0) {
            from = SEEN.getOrDefault(key, 0);
            startedAt = now;
        } else if (level != this.level) {
            from = shownFor(this.level, now);
            startedAt = now;
        }
        this.level = level;
        SEEN.put(key, level);
    }

    /** Applies this board's transition to the frost drawn from now until another board begins. */
    public void begin() {
        drawing = this;
    }

    /** Stops applying any board's transition, e.g. when its screen closes. */
    public static void clear(@Nullable FrostFade fade) {
        if (fade == null || drawing == fade) drawing = null;
    }

    /**
     * 一张纸自己的霜冻等级是 {@code level} 时，现在该画多少霜。整块板一起移动，所以比板子轻一级的小纸也同步化开。
     * How much frost to draw now for a sheet whose own level is {@code level}. The whole board shifts together, so the
     * lighter sheets thaw in step with the header.
     */
    public static float shown(int level) {
        FrostFade fade = drawing;
        return fade == null ? level : fade.shownFor(level, Util.getMillis());
    }

    private float shownFor(int sheet, long now) {
        if (startedAt < 0 || from == level) return sheet;
        float t = (now - startedAt) / MILLIS;
        if (t >= 1) return sheet;
        float eased = 1 - (1 - t) * (1 - t) * (1 - t);
        return Mth.clamp(sheet + (from - level) * (1 - eased), 0f, 3f);
    }
}

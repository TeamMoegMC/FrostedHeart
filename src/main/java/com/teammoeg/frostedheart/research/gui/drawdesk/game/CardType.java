/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.research.gui.drawdesk.game;

import java.util.function.BiFunction;

public enum CardType {
    NONE((a, b) -> false, false),
    SIMPLE((a, b) -> a == b || a == 0 || b == 0, true),
    PAIR((a, b) -> a / 2 == b / 2 && a != b, false),
    ADDING((a, b) -> (a == 0 || b == 0) && a != b, false);
    final BiFunction<Integer, Integer, Boolean> matcher;
    final boolean mustInPair;

    private CardType(BiFunction<Integer, Integer, Boolean> matcher, boolean mustInPair) {
        this.matcher = matcher;
        this.mustInPair = mustInPair;
    }

    public boolean isGood(int num) {
        return !mustInPair || num % 2 == 0;
    }

    public boolean match(int othis, int othat) {
        return matcher.apply(othis, othat);
    }
}
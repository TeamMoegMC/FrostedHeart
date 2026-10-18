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

package com.teammoeg.frostedheart.content.town.transport;

/** Shared client-side rate adjustment rules for town transport consumers. */
public final class TransportRateScroll {
    private TransportRateScroll() {
    }

    public static int increment(boolean shiftDown, boolean ctrlDown) {
        if (shiftDown && ctrlDown) {
            return 64;
        }
        if (ctrlDown) {
            return 16;
        }
        return shiftDown ? 8 : 1;
    }

    public static int adjust(
            int currentRate,
            int scrollSteps,
            boolean shiftDown,
            boolean ctrlDown,
            int maximumRate
    ) {
        long adjusted = (long) currentRate
                + (long) scrollSteps * increment(shiftDown, ctrlDown);
        return (int) Math.max(0L, Math.min(maximumRate, adjusted));
    }

    public static int rateForScroll(String text, int acceptedRate, int maximumRate) {
        try {
            int parsed = Integer.parseInt(text);
            if (parsed >= 0 && parsed <= maximumRate) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
        }
        return Math.max(0, Math.min(maximumRate, acceptedRate));
    }
}

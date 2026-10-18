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

package com.teammoeg.frostedheart.content.ui.tips;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Pure queue transformations used by the client Tip overlay. */
public final class TipQueueModel {
    private TipQueueModel() {
    }

    public static <T> List<T> enqueue(
            List<T> queue,
            T incoming,
            Function<T, String> id
    ) {
        if (containsId(queue, id.apply(incoming), id)) return List.copyOf(queue);
        List<T> result = new ArrayList<>(queue);
        result.add(incoming);
        return List.copyOf(result);
    }

    /**
     * Inserts an urgent item and keeps exactly one resumable copy of the
     * interrupted item after the overlay removes its fading-out copy.
     */
    public static <T> List<T> preempt(
            List<T> queue,
            T current,
            T incoming,
            Function<T, String> id
    ) {
        String incomingId = id.apply(incoming);
        List<T> result = new ArrayList<>(queue);
        result.removeIf(item -> id.apply(item).equals(incomingId));

        if (current != null) {
            String currentId = id.apply(current);
            int resumeIndex = 0;
            while (resumeIndex < result.size()
                    && !id.apply(result.get(resumeIndex)).equals(currentId)) {
                resumeIndex++;
            }
            if (resumeIndex == result.size()) resumeIndex = 0;
            result.removeIf(item -> id.apply(item).equals(currentId));
            resumeIndex = Math.min(resumeIndex, result.size());
            result.add(resumeIndex, current);
            result.add(resumeIndex, current);
        }
        result.add(0, incoming);
        return List.copyOf(result);
    }

    private static <T> boolean containsId(List<T> queue, String wanted, Function<T, String> id) {
        return queue.stream().anyMatch(item -> id.apply(item).equals(wanted));
    }
}

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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class TipQueueModelTest {
    @Test
    void consecutivePreemptionsResumeInterruptedTutorialExactlyOnce() {
        List<String> queue = List.of("tutorial", "ordinary-next");
        queue = TipQueueModel.preempt(queue, "tutorial", "critical-one", value -> value);
        queue = TipQueueModel.preempt(queue, "tutorial", "critical-two", value -> value);

        List<String> afterFade = new ArrayList<>(queue);
        afterFade.remove("tutorial");
        assertEquals(List.of("critical-two", "critical-one", "tutorial", "ordinary-next"),
                afterFade);
    }

    @Test
    void preemptingAnAlreadyVisibleAlarmResumesItBeforeOlderQueueItems() {
        List<String> queue = List.of("critical-two", "critical-one", "tutorial");
        queue = TipQueueModel.preempt(queue, "critical-two", "critical-three", value -> value);

        List<String> afterFade = new ArrayList<>(queue);
        afterFade.remove("critical-two");
        assertEquals(List.of("critical-three", "critical-two", "critical-one", "tutorial"),
                afterFade);
    }

    @Test
    void normalQueueUsesIdentifiersToRejectDuplicates() {
        assertEquals(List.of("one", "two"), TipQueueModel.enqueue(
                List.of("one", "two"), "two", value -> value));
    }
}

/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.ui.tips;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

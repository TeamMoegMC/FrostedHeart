/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.gui.knowledge;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeLabLayoutTest {
    @Test
    void responsiveColumnsNeverOverlapAtSupportedMinimum() {
        assertNonOverlapping(KnowledgeLabLayout.calculate(280, 188));
    }

    @Test
    void wideWindowKeepsReadableDetailColumn() {
        KnowledgeLabLayout layout = KnowledgeLabLayout.calculate(912, 492);
        assertNonOverlapping(layout);
        assertTrue(layout.detail().width() >= 300);
    }

    private static void assertNonOverlapping(KnowledgeLabLayout layout) {
        assertTrue(layout.list().right() < layout.detail().x());
        assertTrue(layout.detail().right() < layout.context().x());
        assertTrue(layout.context().right() <= layout.header().right());
        assertTrue(layout.list().y() >= layout.header().bottom());
    }
}

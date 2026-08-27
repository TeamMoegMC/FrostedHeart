/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.gui.drawdesk;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DrawDeskLayoutTest {
    @Test
    void knowledgeLabEntryStaysAbovePlayerInventory() {
        assertTrue(
                DrawDeskLayer.KNOWLEDGE_BUTTON_Y + DrawDeskLayer.KNOWLEDGE_BUTTON_HEIGHT
                        <= DrawDeskContainer.PLAYER_INVENTORY_TOP,
                "knowledge-lab entry must not overlap player inventory slots");
    }
}

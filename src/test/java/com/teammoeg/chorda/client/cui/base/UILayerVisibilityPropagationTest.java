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

package com.teammoeg.chorda.client.cui.base;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UILayerVisibilityPropagationTest {
    @Test
    void hiddenSubtreeSkipsFramePropagationAndRestoresOnTheSameFrameItIsShown() {
        TestLayer root = new TestLayer(null);
        TestLayer subtree = new TestLayer(root);
        CountingElement child = new CountingElement(subtree);
        root.add(subtree);
        subtree.add(child);
        root.setSize(100, 100);
        subtree.setSize(100, 100);
        child.setSize(100, 100);

        subtree.setVisible(false);
        root.updateRenderInfo(0, 0, 10.0D, 10.0D, 0.0F);
        root.updateMouseOver();
        assertEquals(0, child.renderInfoUpdates);
        assertEquals(0, child.mouseOverUpdates);
        assertFalse(subtree.isMouseOver());

        subtree.setVisible(true);
        root.updateRenderInfo(0, 0, 10.0D, 10.0D, 0.0F);
        root.updateMouseOver();
        assertEquals(1, child.renderInfoUpdates);
        assertEquals(1, child.mouseOverUpdates);
        assertTrue(subtree.isMouseOver());
        assertTrue(child.isMouseOver());
    }

    private static final class CountingElement extends UIElement {
        private int renderInfoUpdates;
        private int mouseOverUpdates;

        private CountingElement(UIElement parent) {
            super(parent);
        }

        @Override
        public void updateRenderInfo(int x, int y, double mouseX, double mouseY, float partialTick) {
            renderInfoUpdates++;
            super.updateRenderInfo(x, y, mouseX, mouseY, partialTick);
        }

        @Override
        public void updateMouseOver() {
            mouseOverUpdates++;
            super.updateMouseOver();
        }
    }

    private static final class TestLayer extends UILayer {
        private TestLayer(UIElement parent) {
            super(parent);
        }

        @Override
        public void addUIElements() {
        }

        @Override
        public void alignWidgets() {
        }
    }
}

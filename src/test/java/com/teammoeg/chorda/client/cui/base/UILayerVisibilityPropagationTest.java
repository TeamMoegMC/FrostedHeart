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

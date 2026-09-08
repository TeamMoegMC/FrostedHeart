/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.client;

import com.teammoeg.chorda.client.cui.base.*;
import com.teammoeg.chorda.client.cui.theme.Theme;
import com.teammoeg.frostedresearch.knowledge.client.ui.JournalButton;
import com.teammoeg.frostedresearch.knowledge.client.ui.JournalRelation;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeLayoutTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void narrowAndWidePagesKeepColumnsAndActionsInsideThePaper() {
        for (int width : new int[]{296, 320, 396, 456, 660})
            for (int height : new int[]{210, 246, 360})
                for (boolean tray : new boolean[]{false, true}) {
                    var layout = KnowledgeLayout.of(width, height, tray);
                    assertTrue(layout.rightX() > 14 + layout.left());
                    assertTrue(layout.rightX() + layout.rightWidth() <= width - 14);
                    assertTrue(layout.listTop() < layout.bottom());
                    assertTrue(layout.bottom() < height);
                }
    }

    @Test
    void emptyJournalBuildsActualCuiControlsWithoutRequiringAFocusedRecord() {
        var parent = new TestParent();
        var layer = new KnowledgeLayer(parent, null, () -> {
        }, () -> {
        });
        for (int width : new int[]{296, 456, 660}) {
            layer.resize(width, 246);
            long buttons = layer.getElements().stream().filter(JournalButton.class::isInstance).count();
            assertTrue(buttons >= 12, "CUI build must reach the reading actions, not stop on an empty selection");
            for (UIElement child : layer.getElements())
                if (child.isVisible()) {
                    assertTrue(child.getX() >= 0 && child.getY() >= 0);
                    assertTrue(child.getX() + child.getWidth() <= layer.getWidth());
                    assertTrue(child.getY() + child.getHeight() <= layer.getHeight());
                }
        }
    }

    @Test
    void relationGeometryFitsFiveInputsOnNarrowPages() {
        for (int width : new int[]{137, 149, 273, 450})
            for (int inputs = 2; inputs <= 5; inputs++) {
                var layout = JournalRelation.Geometry.fit(width, 110, inputs, 1);
                assertTrue(layout.columns() * layout.cardWidth() + (layout.columns() - 1) * 7 <= width);
                assertTrue(layout.cardHeight() >= 18);
                assertTrue(layout.contentHeight() <= 126);
            }
        assertTrue(JournalRelation.Geometry.fit(149, 100, 5, 12).contentHeight() > 100);
    }

    private static final class TestParent extends UIElement {
        TestParent() {
            super(null);
        }

        @Override
        public LayerHolder getLayerHolder() {
            return HOLDER;
        }
    }

    private static final LayerHolder HOLDER = new LayerHolder() {
        @Override
        public void focusOn(UIElement e) {
        }

        @Override
        public Font getFont() {
            return null;
        }

        @Override
        public void refreshElements() {
        }

        @Override
        public Theme theme() {
            return null;
        }

        @Override
        public boolean shouldRenderGradient() {
            return false;
        }

        @Override
        public boolean onCloseQuery() {
            return true;
        }

        @Override
        public Screen getPrevScreen() {
            return null;
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void closeGui(boolean previous) {
        }

        @Override
        public void updateGui(int x, int y, double mx, double my, float pt) {
        }
    };
}

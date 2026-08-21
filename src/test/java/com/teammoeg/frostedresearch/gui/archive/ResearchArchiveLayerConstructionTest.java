package com.teammoeg.frostedresearch.gui.archive;

import com.teammoeg.chorda.client.cui.base.LayerHolder;
import com.teammoeg.chorda.client.cui.base.UIElement;
import com.teammoeg.chorda.client.cui.theme.Theme;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchArchiveLayerConstructionTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void initializesSearchTextAfterItsCallbackDependencies() {
        ResearchOpenContext context = ResearchOpenContext.drawingDesk(null);
        ResearchWorkspaceState state = new ResearchWorkspaceState(context);
        ResearchNavigationController navigation = new StatefulResearchNavigationController(
                context, state, () -> { });

        assertDoesNotThrow(() -> new ResearchArchiveLayer(
                new TestParent(), context, state, navigation, () -> { }));
    }

    @Test
    void normalArchiveOnlyIncludesDiscoveredDefinitions() {
        assertFalse(ResearchArchiveLayer.definitionVisible(false, true, true, true, true));
        assertFalse(ResearchArchiveLayer.definitionVisible(false, false, false, false, false));
        assertTrue(ResearchArchiveLayer.definitionVisible(false, false, true, false, false));
        assertTrue(ResearchArchiveLayer.definitionVisible(false, false, false, true, false));
        assertTrue(ResearchArchiveLayer.definitionVisible(false, false, false, false, true));
    }

    @Test
    void editorArchiveIncludesAllDefinitions() {
        assertTrue(ResearchArchiveLayer.definitionVisible(true, true, false, false, false));
    }

    @Test
    void openingProjectWorkspaceHidesAndDisablesGraphViewport() throws ReflectiveOperationException {
        ResearchOpenContext context = ResearchOpenContext.drawingDesk(null);
        ResearchWorkspaceState state = new ResearchWorkspaceState(context);
        ResearchArchiveLayer layer = new ResearchArchiveLayer(
                new TestParent(), context, state,
                new StatefulResearchNavigationController(context, state, () -> { }), () -> { });

        state.selectResearch("project");
        state.setProjectWorkspaceOpen(true);
        layer.resizeArchive(640, 360);
        UIElement graphViewport = privateElement(layer, "graphViewport");
        assertFalse(graphViewport.isVisible());
        assertFalse(graphViewport.isEnabled());

        state.setProjectWorkspaceOpen(false);
        layer.resizeArchive(640, 360);
        assertTrue(graphViewport.isVisible());
        assertTrue(graphViewport.isEnabled());
    }

    private static UIElement privateElement(ResearchArchiveLayer layer, String fieldName)
            throws ReflectiveOperationException {
        Field field = ResearchArchiveLayer.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (UIElement) field.get(layer);
    }

    private static final class TestParent extends UIElement {
        private TestParent() {
            super(null);
        }

        @Override
        public LayerHolder getLayerHolder() {
            return TEST_LAYER_HOLDER;
        }
    }

    private static final LayerHolder TEST_LAYER_HOLDER = new LayerHolder() {
        @Override
        public void focusOn(UIElement element) {
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
        public void closeGui(boolean openPrevScreen) {
        }

        @Override
        public void updateGui(int offX, int offY, double mouseX, double mouseY, float partialTick) {
        }
    };
}

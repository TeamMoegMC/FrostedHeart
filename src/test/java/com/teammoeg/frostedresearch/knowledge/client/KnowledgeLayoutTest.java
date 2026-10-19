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
import com.teammoeg.frostedresearch.knowledge.KnowledgeService;
import com.teammoeg.frostedresearch.knowledge.ResearchResult;
import com.teammoeg.frostedresearch.knowledge.definition.*;
import com.teammoeg.frostedresearch.knowledge.link.FieldCondition;
import com.teammoeg.frostedresearch.knowledge.link.LinkRule;
import com.teammoeg.frostedresearch.knowledge.model.*;
import com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource;
import com.teammoeg.frostedresearch.knowledge.state.KnowledgeState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import org.junit.jupiter.api.*;

import java.util.*;

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
    void researchPageFitsAnOpenRunAndAProjectNotYetStarted() {
        MinecraftForge.EVENT_BUS.start();
        ResourceLocation origin = new ResourceLocation("test", "origin"), survey = new ResourceLocation("test", "survey"),
                later = new ResourceLocation("test", "later"), finding = new ResourceLocation("test", "finding");
        var rock = new LinkRule.Slot("a", KnowledgeKey.Kind.OBSERVATION, Optional.empty(), Optional.of(Observation.Type.BLOCK),
                List.of(new FieldCondition("object", FieldCondition.Operator.EQ, Optional.of(ObservationValue.known("test:rock")), Optional.empty())));
        var result = new ResultDefinition(new ResearchResult.Finding(finding, List.of()), "Finding", "", List.of(), true);
        KnowledgeDefinitions.install(new KnowledgeDefinitions.Snapshot(0,
                Map.of(origin, new IdeaDefinition("Origin", "", List.of(), List.of(survey, later), true)), Map.of(finding, result), Map.of(), Map.of(),
                Map.of(survey, new ProjectDefinition("Survey", "Look at rocks.", List.of(finding), List.of(
                                ProjectRequirement.evidence("rock", "A rock", List.of(rock), List.of(), List.of()),
                                ProjectRequirement.calculation("calc", "", 1, 2))),
                        later, new ProjectDefinition("Later", "", List.of(finding), List.of(ProjectRequirement.calculation("calc", "", 0, 1)))), List.of()));
        var service = new KnowledgeService(UUID.randomUUID(), new KnowledgeState(), 1, () -> {
        });
        var source = new AcquisitionSource("test", Optional.empty(), 1);
        service.learn(KnowledgeElement.idea(origin), source, KnowledgeService.Grant.NORMAL);
        service.learn(KnowledgeElement.observation(new Observation(UUID.randomUUID(), Observation.Type.BLOCK, new ResourceLocation("test", "rock"),
                Map.of(), Observation.Source.command(), Optional.empty())), source, KnowledgeService.Grant.NORMAL);
        UUID run = service.startResearch(survey, KnowledgeKey.idea(origin)).orElseThrow();
        service.submitEvidence(run, "rock");
        KnowledgeClientState.setSnapshot(service.snapshot());
        try {
            var layer = new KnowledgeLayer(new TestParent(), null, () -> {
            }, () -> {
            });
            for (int width : new int[]{296, 456, 660})
                for (String project : List.of(survey.toString(), later.toString())) {
                    layer.resize(width, 246);
                    layer.openResearch(project);
                    long buttons = layer.getElements().stream().filter(JournalButton.class::isInstance).filter(UIElement::isVisible).count();
                    assertTrue(buttons >= 7, "bookmarks, return and the research actions are built for " + project);
                    for (UIElement child : layer.getElements())
                        if (child.isVisible()) {
                            assertTrue(child.getX() >= 0 && child.getY() >= 0, project + " at " + width);
                            assertTrue(child.getX() + child.getWidth() <= layer.getWidth(), project + " at " + width);
                            assertTrue(child.getY() + child.getHeight() <= layer.getHeight(), project + " at " + width);
                        }
                }
        } finally {
            KnowledgeClientState.setSnapshot(new CompoundTag());
            KnowledgeDefinitions.clearForTests();
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

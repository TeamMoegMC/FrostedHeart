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

package com.teammoeg.frostedresearch.knowledge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeSyncSnapshotTest {
    @Test
    void snapshotRoundTripsRevisionProjectionsAndViewWithoutTeamState() {
        ResourceLocation finding = id("finding");
        ResourceLocation design = id("design");
        ResourceLocation construction = id("construction");
        ResourceLocation procedure = id("procedure");
        ResourceLocation recipe = id("recipe");
        ResourceLocation multiblock = id("multiblock");
        ResourceLocation block = id("block");

        AccessSource.ResultSource findingSource = source(ResearchResult.ResultType.FINDING, finding);
        KnowledgeProjection knowledge = new KnowledgeProjection(List.of(
                new KnowledgeProjection.FindingEntry(finding, List.of(id("finding_view")), findingSource)));
        TechnologyAccessProjection technology = TechnologyAccessProjection.create(
                Set.of(recipe), Set.of(multiblock), Set.of(block),
                Map.of(recipe, List.of(source(ResearchResult.ResultType.DESIGN, design))),
                Map.of(multiblock, List.of(source(ResearchResult.ResultType.CONSTRUCTION, construction))),
                Map.of(block, List.of(source(ResearchResult.ResultType.PROCEDURE, procedure))));
        CompoundTag view = new CompoundTag();
        view.put("archive", StringTag.valueOf("visible entries"));
        KnowledgeSyncSnapshot original = new KnowledgeSyncSnapshot(42, knowledge, technology, view);

        CompoundTag encoded = (CompoundTag) KnowledgeSyncSnapshot.CODEC.encodeStart(NbtOps.INSTANCE, original)
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        // The persisted team state stays on the server; the client only needs the view and the projections.
        assertEquals(Set.of("catalog_revision", "knowledge", "technology", "archive_view"), encoded.getAllKeys());
        KnowledgeSyncSnapshot decoded = KnowledgeSyncSnapshot.CODEC.parse(NbtOps.INSTANCE, encoded)
                .getOrThrow(false, message -> { throw new AssertionError(message); });

        assertEquals(42, decoded.catalogRevision());
        assertTrue(decoded.knowledge().hasFinding(finding));
        assertEquals(List.of(findingSource), List.of(decoded.knowledge().finding(finding).source()));
        assertTrue(decoded.technology().recipe(recipe).allowed());
        assertTrue(decoded.technology().multiblock(multiblock).allowed());
        assertTrue(decoded.technology().block(block).allowed());
        assertEquals(view, decoded.archiveView());
    }

    private static AccessSource.ResultSource source(ResearchResult.ResultType type, ResourceLocation resultId) {
        return new AccessSource.ResultSource(id("topic"), type, resultId);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("test", path);
    }
}

package com.teammoeg.frostedresearch.knowledge;

import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeSyncSnapshotTest {
    @Test
    void fullSnapshotRoundTripsAcquiredStateRevisionAndCompiledProjections() {
        ResourceLocation finding = id("finding");
        ResourceLocation design = id("design");
        ResourceLocation construction = id("construction");
        ResourceLocation procedure = id("procedure");
        ResourceLocation recipe = id("recipe");
        ResourceLocation multiblock = id("multiblock");
        ResourceLocation block = id("block");

        TeamKnowledgeData teamData = new TeamKnowledgeData();
        teamData.acquireFinding(finding);
        teamData.acquireDesign(design);
        teamData.acquireConstruction(construction);
        teamData.acquireProcedure(procedure);

        AccessSource.ResultSource findingSource = source(ResearchResult.ResultType.FINDING, finding);
        KnowledgeProjection knowledge = new KnowledgeProjection(List.of(
                new KnowledgeProjection.FindingEntry(finding, List.of(id("finding_view")), findingSource)));
        TechnologyAccessProjection technology = TechnologyAccessProjection.create(
                Set.of(recipe), Set.of(multiblock), Set.of(block),
                Map.of(recipe, List.of(source(ResearchResult.ResultType.DESIGN, design))),
                Map.of(multiblock, List.of(source(ResearchResult.ResultType.CONSTRUCTION, construction))),
                Map.of(block, List.of(source(ResearchResult.ResultType.PROCEDURE, procedure))));
        KnowledgeSyncSnapshot original = new KnowledgeSyncSnapshot(42, teamData, knowledge, technology);

        Tag encoded = KnowledgeSyncSnapshot.CODEC.encodeStart(NbtOps.INSTANCE, original)
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        KnowledgeSyncSnapshot decoded = KnowledgeSyncSnapshot.CODEC.parse(NbtOps.INSTANCE, encoded)
                .getOrThrow(false, message -> { throw new AssertionError(message); });

        assertEquals(42, decoded.catalogRevision());
        assertEquals(teamData.findingIds(), decoded.teamData().findingIds());
        assertEquals(teamData.designIds(), decoded.teamData().designIds());
        assertEquals(teamData.constructionIds(), decoded.teamData().constructionIds());
        assertEquals(teamData.procedureIds(), decoded.teamData().procedureIds());
        assertTrue(decoded.knowledge().hasFinding(finding));
        assertEquals(List.of(findingSource), List.of(decoded.knowledge().finding(finding).source()));
        assertTrue(decoded.technology().recipe(recipe).allowed());
        assertTrue(decoded.technology().multiblock(multiblock).allowed());
        assertTrue(decoded.technology().block(block).allowed());
    }

    private static AccessSource.ResultSource source(ResearchResult.ResultType type, ResourceLocation resultId) {
        return new AccessSource.ResultSource(id("topic"), type, resultId);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("test", path);
    }
}

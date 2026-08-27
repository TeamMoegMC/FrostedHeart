package com.teammoeg.frostedresearch.knowledge;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericResearchWorkflowTest {
    private static final ResourceLocation TOPIC = id("block_state_comparison");
    private static final ResourceLocation IDEA = id("block_state_question");

    @AfterEach
    void clearCatalog() {
        ResearchResultCatalog.clearForTests();
    }

    @Test
    void dataOnlyNonGeologyTopicUsesTheGenericRuntimeHandlers() throws Exception {
        ResearchTopicDefinition topic;
        try (var stream = getClass().getResourceAsStream(
                "/data/frostedresearch_test/frostedresearch/topics/block_state_comparison.json")) {
            assertTrue(stream != null);
            topic = ResearchTopicDefinition.CODEC.parse(JsonOps.INSTANCE,
                            JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
                    .getOrThrow(false, message -> { throw new AssertionError(message); });
        }
        ResearchResultCatalog.install(new ResearchResultCatalog.Candidate(Map.of(TOPIC, topic), Map.of()));

        TeamKnowledgeData data = new TeamKnowledgeData();
        UUID observer = UUID.randomUUID();
        ResourceLocation blockKind = new ResourceLocation("frostedresearch", "block_observation");
        ResourceLocation dimension = new ResourceLocation("minecraft", "overworld");
        ResourceLocation furnace = new ResourceLocation("minecraft", "furnace");
        ResourceLocation facet = new ResourceLocation("frostedresearch", "block_observation");
        ResourceLocation channel = new ResourceLocation("frostedresearch", "research_notebook");
        KnowledgeRecord unlit = KnowledgeRecord.create(KnowledgeRecord.Type.BLOCK, blockKind, dimension,
                BlockPos.ZERO, furnace, Map.of("lit", "false"), 1, observer, Set.of(facet), Set.of(channel),
                Optional.empty(), "test|furnace|unlit");
        KnowledgeRecord lit = KnowledgeRecord.create(KnowledgeRecord.Type.BLOCK, blockKind, dimension,
                BlockPos.ZERO, furnace, Map.of("lit", "true"), 2, observer, Set.of(facet), Set.of(channel),
                Optional.empty(), "test|furnace|lit");
        data.archiveObservation(unlit);
        data.archiveObservation(lit);

        assertTrue(ResearchWorkflowRegistry.actionCards(data).isEmpty(),
                "a topic must not leak action cards before an Idea exists");
        List<IdeaCandidate> candidates = ResearchWorkflowRegistry.findCandidates(data, Set.of(unlit.id(), lit.id()));
        assertEquals(1, candidates.size());
        assertEquals(IDEA, candidates.get(0).ideaId());

        IdeaRecord idea = IdeaRecord.create(TOPIC, IDEA, "drawing_desk", candidates.get(0).evidence(), 3);
        data.recordIdea(idea);
        List<ActionCard> actions = ResearchWorkflowRegistry.actionCards(data);
        assertEquals(1, actions.size());
        assertEquals(new ResourceLocation("frostedresearch", "compare_records"), actions.get(0).actionId());

        UUID artifactId = UUID.randomUUID();
        data.appendComparison(new FieldComparisonArtifact(artifactId, TOPIC, idea.id(), unlit.id(), lit.id(),
                FieldComparisonArtifact.Outcome.MATCH, 4));
        IdeaRecord ready = idea.withState(IdeaRecord.State.READY, 4);
        data.updateIdea(ready);
        ResearchTopicDefinition.Resolution resolution = topic.resolution().orElseThrow();
        ResolutionHandler handler = ResearchWorkflowRegistry.resolution(resolution.resolver());
        assertTrue(handler.canResolve(TOPIC, topic, resolution, data, ready));

        TeamKnowledgeData unrelatedReport = new TeamKnowledgeData();
        unrelatedReport.recordIdea(ready);
        unrelatedReport.appendComparison(new FieldComparisonArtifact(UUID.randomUUID(), TOPIC,
                UUID.randomUUID(), unlit.id(), lit.id(), FieldComparisonArtifact.Outcome.MATCH, 4));
        assertFalse(handler.canResolve(TOPIC, topic, resolution, unrelatedReport, ready),
                "a MATCH report from another Idea must not resolve this Idea");
        assertFalse(ResearchWorkflowRegistry.actionCards(new TeamKnowledgeData()).stream()
                .anyMatch(card -> card.topicId().equals(TOPIC)));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("frostedresearch_test", path);
    }
}

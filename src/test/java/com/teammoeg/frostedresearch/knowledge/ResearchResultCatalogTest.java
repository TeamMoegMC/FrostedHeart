package com.teammoeg.frostedresearch.knowledge;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.crafting.RecipeManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResearchResultCatalogTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        com.teammoeg.frostedheart.content.utility.oredetect.GeologyResearchIntegration.register();
    }

    @AfterEach
    void clear() {
        ResearchResultCatalog.clearForTests();
    }

    @Test
    void emptyCatalogIsValidAndRevisionsAreMonotonic() {
        ResearchResultCatalog.Snapshot first = ResearchResultCatalog.install(
                new ResearchResultCatalog.Candidate(Map.of(), Map.of()));
        ResearchResultCatalog.Snapshot second = ResearchResultCatalog.install(
                new ResearchResultCatalog.Candidate(Map.of(), Map.of()));
        assertTrue(second.revision() > first.revision());
        assertTrue(second.results().isEmpty());
    }

    @Test
    void validationAggregatesDuplicateResultsAndDanglingProfiles() {
        ResourceLocation duplicate = id("duplicate");
        Map<ResourceLocation, ResearchTopicDefinition> topics = new LinkedHashMap<>();
        topics.put(id("one"), topic(new ResearchResult.Finding(duplicate, List.of())));
        topics.put(id("two"), topic(new ResearchResult.Prototype(duplicate, id("missing_profile"))));
        List<String> diagnostics = new ArrayList<>();
        ResearchResultCatalogLoader.validate(topics, Map.of(), new RecipeManager(), diagnostics);
        assertTrue(diagnostics.stream().anyMatch(message -> message.contains("duplicate result id")));
        assertTrue(diagnostics.stream().anyMatch(message -> message.contains("unknown prototype profile")));
    }

    @Test
    void validationAggregatesEmptyDuplicateAndMissingTargets() {
        Map<ResourceLocation, ResearchTopicDefinition> topics = Map.of(id("invalid"),
                new ResearchTopicDefinition(3, ResearchTopicDefinition.Presentation.EMPTY,
                        List.of(
                                new ResearchResult.Design(id("empty_design"), List.of()),
                                new ResearchResult.Design(id("missing_recipe"),
                                        List.of(id("no_recipe"), id("no_recipe"))),
                                new ResearchResult.Construction(id("empty_construction"), List.of()),
                                new ResearchResult.Construction(id("missing_multiblock"),
                                        List.of(id("no_multiblock"))),
                                new ResearchResult.Procedure(id("empty_procedure"), List.of()),
                                new ResearchResult.Procedure(id("missing_block"), List.of(id("no_block")))),
                        List.of(new ResearchTopicDefinition.ItemReward(id("no_item"), 0))));
        List<String> diagnostics = new ArrayList<>();
        ResearchResultCatalogLoader.validate(topics,
                Map.of(id("bad_profile"), new PrototypeProfileDefinition(2, 0)),
                new RecipeManager(), diagnostics);

        assertTrue(diagnostics.stream().anyMatch(message -> message.contains("recipes must not be empty")));
        assertTrue(diagnostics.stream().anyMatch(message -> message.contains("duplicate recipe")));
        assertTrue(diagnostics.stream().anyMatch(message -> message.contains("unknown recipe")));
        assertTrue(diagnostics.stream().anyMatch(message -> message.contains("multiblocks must not be empty")));
        assertTrue(diagnostics.stream().anyMatch(message -> message.contains("unknown multiblock")));
        assertTrue(diagnostics.stream().anyMatch(message -> message.contains("usable_blocks must not be empty")));
        assertTrue(diagnostics.stream().anyMatch(message -> message.contains("unknown block")));
        assertTrue(diagnostics.stream().anyMatch(message -> message.contains("unknown reward item")));
        assertTrue(diagnostics.stream().anyMatch(message -> message.contains("reward count must be positive")));
        assertTrue(diagnostics.stream().anyMatch(message -> message.contains("prototype format must be 1")));
        assertTrue(diagnostics.stream().anyMatch(message -> message.contains("prototype revision must be positive")));
    }

    @Test
    void rejectedCandidateCannotReplaceLastKnownGoodSnapshot() {
        ResearchResultCatalog.Snapshot good = ResearchResultCatalog.install(
                new ResearchResultCatalog.Candidate(Map.of(id("good"),
                        topic(new ResearchResult.Finding(id("finding"), List.of()))), Map.of()));
        List<String> diagnostics = new ArrayList<>();
        Map<ResourceLocation, ResearchTopicDefinition> invalid = Map.of(id("bad"),
                new ResearchTopicDefinition(2, ResearchTopicDefinition.Presentation.EMPTY,
                        List.of(), List.of()));
        ResearchResultCatalogLoader.validate(invalid, Map.of(), new RecipeManager(), diagnostics);
        assertFalse(diagnostics.isEmpty());
        assertSame(good, ResearchResultCatalog.current());
        assertEquals(id("finding"), good.result(id("finding")).result().id());
    }

    @Test
    void workflowTagsValidateAgainstCurrentReloadResourceUniverse() {
        ResourceLocation stone = new ResourceLocation("forge", "stone");
        ResearchTopicDefinition topic = new ResearchTopicDefinition(
                3,
                ResearchTopicDefinition.Presentation.EMPTY,
                List.of(new ResearchResult.Finding(id("finding"), List.of())),
                List.of(),
                ResearchTopicDefinition.Legacy.NONE,
                List.of(new ResearchTopicDefinition.IdeaSource(
                        new ResourceLocation("frostedheart", "field_evidence"), id("idea"), List.of(stone))),
                Optional.empty(),
                List.of(),
                Optional.empty());
        List<String> accepted = new ArrayList<>();
        ResearchResultCatalogLoader.validate(
                Map.of(id("topic"), topic), Map.of(), new RecipeManager(), accepted, Set.of(stone));
        assertFalse(accepted.stream().anyMatch(message -> message.contains("unknown block tag")));

        List<String> rejected = new ArrayList<>();
        ResearchResultCatalogLoader.validate(
                Map.of(id("topic"), topic), Map.of(), new RecipeManager(), rejected, Set.of());
        assertTrue(rejected.stream().anyMatch(message -> message.contains("unknown block tag forge:stone")));
    }

    private static ResearchTopicDefinition topic(ResearchResult result) {
        return new ResearchTopicDefinition(3, ResearchTopicDefinition.Presentation.EMPTY,
                List.of(result), List.of());
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("test", path);
    }
}

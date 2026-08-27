package com.teammoeg.frostedresearch.data;

import com.teammoeg.frostedheart.content.utility.oredetect.OreProspectingModel;
import com.teammoeg.frostedheart.content.utility.oredetect.GeologyBlockObservationProvider;
import com.teammoeg.frostedheart.content.utility.oredetect.GeologyResearchIntegration;
import com.teammoeg.frostedresearch.api.TeamResearchService;
import com.teammoeg.frostedresearch.knowledge.IdeaRecord;
import com.teammoeg.frostedresearch.knowledge.KnowledgeRecord;
import com.teammoeg.frostedresearch.knowledge.FieldComparisonArtifact;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TeamKnowledgeDataPhaseTwoTest {
    private static final ResourceLocation DIMENSION = id("dimension");
    private static final ResourceLocation STONE = new ResourceLocation("minecraft", "stone");

    @Test
    void schemaOnePayloadMigratesToSchemaTwoCollections() {
        net.minecraft.nbt.CompoundTag legacy = new net.minecraft.nbt.CompoundTag();
        legacy.putInt("schemaVersion", 1);
        TeamKnowledgeData decoded = TeamKnowledgeData.CODEC.parse(NbtOps.INSTANCE, legacy)
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        assertTrue(decoded.observations().isEmpty());
        assertTrue(decoded.ideas().isEmpty());
        assertTrue(decoded.comparisons().isEmpty());
    }

    @Test
    void observationsDeduplicateBySectionTypeAndSubjectAndMergeObservers() {
        TeamKnowledgeData data = new TeamKnowledgeData();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        KnowledgeRecord initial = sample(new BlockPos(1, 2, 3), first, 10, OreProspectingModel.Snapshot.EMPTY);
        KnowledgeRecord repeated = sample(new BlockPos(15, 14, 12), second, 20, OreProspectingModel.Snapshot.EMPTY);
        assertTrue(data.archiveObservation(initial));
        assertTrue(data.archiveObservation(repeated));
        assertEquals(1, data.observations().size());
        assertEquals(Set.of(first, second), data.observations().get(0).observers());
        assertEquals(20, data.observations().get(0).lastObserved());
    }

    @Test
    void networkCodecNeverContainsSealedMineralFacts() {
        TeamKnowledgeData data = new TeamKnowledgeData();
        data.archiveObservation(sample(BlockPos.ZERO, UUID.randomUUID(), 1,
                new OreProspectingModel.Snapshot(Map.of(new ResourceLocation("minecraft", "copper_ore"), 4))));
        Tag encoded = TeamKnowledgeData.NETWORK_CODEC.encodeStart(NbtOps.INSTANCE, data)
                .getOrThrow(false, message -> { throw new AssertionError(message); });
        assertFalse(encoded.toString().contains("sealed_facts"));
        assertFalse(encoded.toString().contains("copper_ore"));
    }

    @Test
    void ideaSourcesMergeIdempotently() {
        TeamKnowledgeData data = new TeamKnowledgeData();
        IdeaRecord field = IdeaRecord.create(TeamResearchService.ROCK_TOPIC, TeamResearchService.ROCK_IDEA,
                "field", Set.of(), 1);
        IdeaRecord person = IdeaRecord.create(TeamResearchService.ROCK_TOPIC, TeamResearchService.ROCK_IDEA,
                "person", Set.of(), 2);
        assertTrue(data.recordIdea(field));
        assertTrue(data.recordIdea(person));
        assertFalse(data.recordIdea(person));
        assertEquals(Set.of("field", "person"), data.ideas().get(0).sources());
    }

    @Test
    void actionCardsStayHiddenUntilAnIdeaExists() {
        var cards = TeamResearchService.actionCards(new TeamKnowledgeData(), TeamResearchService.ROCK_TOPIC);
        assertTrue(cards.isEmpty());
    }

    @Test
    void comparisonHasAllThreeOutcomes() {
        assertEquals(FieldComparisonArtifact.Outcome.MATCH,
                TeamResearchService.compareSignals(true, false));
        assertEquals(FieldComparisonArtifact.Outcome.NO_MATCH,
                TeamResearchService.compareSignals(true, true));
        assertEquals(FieldComparisonArtifact.Outcome.INSUFFICIENT,
                TeamResearchService.compareSignals(null, false));
    }

    @Test
    void elementaryGeologyIdeaNeedsOnlyOneOreAndOneStoneWithoutDistanceOrControl() {
        TeamKnowledgeData data = new TeamKnowledgeData();
        UUID observer = UUID.randomUUID();
        KnowledgeRecord ore = KnowledgeRecord.create(KnowledgeRecord.Type.BLOCK,
                KnowledgeRecord.BLOCK_KIND, DIMENSION, new BlockPos(1024, 80, -512),
                new ResourceLocation("example", "tin_ore"), Map.of(), 1, observer,
                Set.of(KnowledgeRecord.BLOCK_OBSERVATION_FACET, GeologyBlockObservationProvider.ORE_FACET),
                Set.of(), Optional.empty(), "ore");
        KnowledgeRecord stone = sample(BlockPos.ZERO, observer, 2, OreProspectingModel.Snapshot.EMPTY);
        data.archiveObservation(ore);
        data.archiveObservation(stone);

        GeologyResearchIntegration.EvidenceSelection selection = GeologyResearchIntegration.selectEvidence(
                data, Set.of(ore.id(), stone.id()), false);

        assertEquals(Set.of(ore.id(), stone.id()), selection.discoveryIds());
        assertTrue(selection.control().isEmpty());
    }

    private static KnowledgeRecord sample(BlockPos pos, UUID observer, long time,
            OreProspectingModel.Snapshot snapshot) {
        return KnowledgeRecord.create(KnowledgeRecord.Type.ROCK_SAMPLE, DIMENSION, pos, STONE,
                time, observer, Optional.of(snapshot));
    }

    private static ResourceLocation id(String path) { return new ResourceLocation("test", path); }
}

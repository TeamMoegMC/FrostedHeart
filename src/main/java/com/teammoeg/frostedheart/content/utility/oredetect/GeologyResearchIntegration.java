/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.utility.oredetect;

import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import com.teammoeg.frostedresearch.knowledge.ActionCard;
import com.teammoeg.frostedresearch.knowledge.FieldComparisonArtifact;
import com.teammoeg.frostedresearch.knowledge.FindingViewHandler;
import com.teammoeg.frostedresearch.knowledge.IdeaCandidate;
import com.teammoeg.frostedresearch.knowledge.IdeaRecord;
import com.teammoeg.frostedresearch.knowledge.KnowledgeProjection;
import com.teammoeg.frostedresearch.knowledge.KnowledgeRecord;
import com.teammoeg.frostedresearch.knowledge.ProtocolHandler;
import com.teammoeg.frostedresearch.knowledge.ResearchTopicDefinition;
import com.teammoeg.frostedresearch.knowledge.ResearchWorkflowRegistry;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationProviderRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Frosted Heart's geology content plugged into the generic research workflow. */
public final class GeologyResearchIntegration {
    public static final ResourceLocation TOPIC = new ResourceLocation("the_winter_rescue", "geology_understanding");
    public static final ResourceLocation IDEA = id("rock_and_ore_signs");
    public static final ResourceLocation FINDING = id("prospecting_signs_indicate_nearby_ore");
    public static final ResourceLocation COPPER_PICK_DESIGN = id("copper_prospecting_pick");

    public static final ResourceLocation FIELD_EVIDENCE = id("field_evidence");
    public static final ResourceLocation PERSON_EXPERIENCE = id("person_experience");
    public static final ResourceLocation DRAWING_DESK = id("drawing_desk");
    public static final ResourceLocation MANUAL_COMPARISON = id("manual_field_comparison");
    public static final ResourceLocation COMPARISON_RESOLUTION = id("field_comparison_resolution");
    public static final ResourceLocation GEOLOGY_ARCHIVE = id("geology_archive");
    public static final ResourceLocation PROSPECTING_DETAIL = id("prospecting_report_detail");

    public static final ResourceLocation ACTION_OUTCROP = id("record_ore");
    public static final ResourceLocation ACTION_NEARBY = id("record_stone");
    public static final ResourceLocation ACTION_COMPARE = id("review_rock_and_ore_notes");
    public static final ResourceLocation ACTION_REVIEW = id("review_result");
    public static final ResourceLocation TRACE_PRESENT = id("ore_trace_present");
    public static final ResourceLocation TRACE_ABSENT = id("ore_trace_absent");

    private GeologyResearchIntegration() {
    }

    public static void register() {
        ObservationProviderRegistry.register(new GeologyBlockObservationProvider());
        ResearchWorkflowRegistry.registerInspirationProvider(DRAWING_DESK);
        ResearchWorkflowRegistry.registerIdeaSource(FIELD_EVIDENCE, (topicId, topic, source, data, pinned) -> {
            EvidenceSelection evidence = selectEvidence(data, pinned, false);
            if (evidence.outcrop().isEmpty() || evidence.nearby().isEmpty()) return Optional.empty();
            return Optional.of(new IdeaCandidate(topicId, source.idea(), evidence.discoveryIds(), "drawing_desk"));
        });
        // Person packages create a direct offer; they do not secretly make a board match.
        ResearchWorkflowRegistry.registerIdeaSource(PERSON_EXPERIENCE,
                (topicId, topic, source, data, pinned) -> Optional.empty());
        ResearchWorkflowRegistry.registerProtocol(MANUAL_COMPARISON, new GeologyProtocol());
        ResearchWorkflowRegistry.registerResolution(COMPARISON_RESOLUTION, (topicId, topic, resolution, data, idea) ->
                idea.state() == IdeaRecord.State.READY);
        ResearchWorkflowRegistry.registerFindingView(new FindingViewHandler() {
            @Override public ResourceLocation id() { return GEOLOGY_ARCHIVE; }
            @Override public List<ResourceLocation> observationAnnotations(
                    TeamKnowledgeData data, KnowledgeRecord record) { return List.of(); }
        });
        ResearchWorkflowRegistry.registerFindingView(new FindingViewHandler() {
            @Override public ResourceLocation id() { return PROSPECTING_DETAIL; }
            @Override public List<ResourceLocation> observationAnnotations(
                    TeamKnowledgeData data, KnowledgeRecord record) {
                if (!isRockSample(record) || record.sealedFacts().isEmpty()) return List.of();
                return List.of(hasCopper(record.sealedFacts().get()) ? TRACE_PRESENT : TRACE_ABSENT);
            }
        });
    }

    public static EvidenceSelection selectEvidence(TeamKnowledgeData data, Set<UUID> allowedIds,
            boolean requireAllowedControl) {
        Optional<KnowledgeRecord> ore = data.observations().stream()
                .filter(record -> allowedIds.contains(record.id())).filter(GeologyResearchIntegration::isOre).findFirst();
        Optional<KnowledgeRecord> stone = data.observations().stream()
                .filter(record -> allowedIds.contains(record.id())).filter(GeologyResearchIntegration::isRockSample)
                .findFirst();
        return new EvidenceSelection(ore, stone, Optional.empty());
    }

    public static EvidenceSelection selectEvidence(TeamKnowledgeData data, Set<UUID> ideaEvidence) {
        return selectEvidence(data, ideaEvidence, false);
    }

    /**
     * Board-created ideas keep their original field pair, while ideas learned
     * from a person can acquire that field basis later from ordinary notebook
     * observations. Topic-specific requirements therefore begin after the
     * idea without turning the notebook or dialogue entry point into a quest.
     */
    private static EvidenceSelection selectIdeaEvidence(TeamKnowledgeData data, IdeaRecord idea) {
        EvidenceSelection recorded = selectEvidence(data, idea.evidence(), false);
        if (recorded.outcrop().isPresent() && recorded.nearby().isPresent()) return recorded;
        Set<UUID> allRecords = data.observations().stream().map(KnowledgeRecord::id)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return selectEvidence(data, allRecords, false);
    }

    public static FieldComparisonArtifact.Outcome compareSamples(KnowledgeRecord nearby, KnowledgeRecord control) {
        if (nearby.sealedFacts().isEmpty() || control.sealedFacts().isEmpty()) {
            return FieldComparisonArtifact.Outcome.INSUFFICIENT;
        }
        return compareSignals(hasCopper(nearby.sealedFacts().get()), hasCopper(control.sealedFacts().get()));
    }

    public static FieldComparisonArtifact.Outcome compareSignals(Boolean nearbyCopper, Boolean controlCopper) {
        if (nearbyCopper == null || controlCopper == null) return FieldComparisonArtifact.Outcome.INSUFFICIENT;
        return nearbyCopper && !controlCopper
                ? FieldComparisonArtifact.Outcome.MATCH : FieldComparisonArtifact.Outcome.NO_MATCH;
    }

    public static boolean hasCopper(OreProspectingModel.Snapshot snapshot) {
        TagKey<Block> copper = BlockTags.create(new ResourceLocation("forge", "ores/copper"));
        return snapshot.mineralCounts().keySet().stream().anyMatch(blockId -> {
            Block block = ForgeRegistries.BLOCKS.getValue(blockId);
            return block != null && block.defaultBlockState().is(copper);
        });
    }

    private static boolean isOre(KnowledgeRecord record) {
        Block block = ForgeRegistries.BLOCKS.getValue(record.subject());
        TagKey<Block> ores = BlockTags.create(new ResourceLocation("forge", "ores"));
        return record.type() == KnowledgeRecord.Type.COPPER_OUTCROP
                || record.publicFacets().contains(GeologyBlockObservationProvider.ORE_FACET)
                || block != null && block.defaultBlockState().is(ores);
    }

    private static boolean isRockSample(KnowledgeRecord record) {
        return record.type() == KnowledgeRecord.Type.ROCK_SAMPLE
                || record.publicFacets().contains(id("rock_sample"));
    }

    public record EvidenceSelection(Optional<KnowledgeRecord> outcrop,
            Optional<KnowledgeRecord> nearby, Optional<KnowledgeRecord> control) {
        public Set<UUID> discoveryIds() {
            Set<UUID> ids = new LinkedHashSet<>();
            outcrop.ifPresent(record -> ids.add(record.id()));
            nearby.ifPresent(record -> ids.add(record.id()));
            return Set.copyOf(ids);
        }
    }

    private static final class GeologyProtocol implements ProtocolHandler {
        @Override
        public List<ActionCard> actions(ResourceLocation topicId, ResearchTopicDefinition topic,
                ResearchTopicDefinition.Protocol protocol, TeamKnowledgeData data, IdeaRecord idea) {
            if (idea.state() == IdeaRecord.State.READY) {
                return List.of(new ActionCard(topicId, protocol.id(), ACTION_REVIEW));
            }
            EvidenceSelection evidence = selectIdeaEvidence(data, idea);
            List<ActionCard> actions = new ArrayList<>();
            if (evidence.outcrop().isEmpty()) {
                actions.add(new ActionCard(topicId, protocol.id(), ACTION_OUTCROP));
            } else if (evidence.nearby().isEmpty()) {
                actions.add(new ActionCard(topicId, protocol.id(), ACTION_NEARBY));
            } else {
                actions.add(new ActionCard(topicId, protocol.id(), ACTION_COMPARE, true));
            }
            return actions;
        }

        @Override
        public Optional<Execution> execute(net.minecraft.server.level.ServerPlayer player,
                ResourceLocation topicId, ResearchTopicDefinition topic,
                ResearchTopicDefinition.Protocol protocol, TeamKnowledgeData data, IdeaRecord idea) {
            EvidenceSelection selection = selectIdeaEvidence(data, idea);
            if (selection.outcrop().isEmpty() || selection.nearby().isEmpty()) return Optional.empty();
            return Optional.of(Execution.readyWithoutArtifact(selection.discoveryIds()));
        }
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("frostedheart", path);
    }
}

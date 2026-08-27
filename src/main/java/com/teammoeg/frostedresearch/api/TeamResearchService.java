/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.api;

import com.teammoeg.chorda.dataholders.team.TeamDataClosure;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import com.teammoeg.frostedresearch.item.UpgradePrototypeItem;
import com.teammoeg.frostedresearch.knowledge.PrototypeProfileDefinition;
import com.teammoeg.frostedresearch.knowledge.ActionCard;
import com.teammoeg.frostedresearch.knowledge.FieldComparisonArtifact;
import com.teammoeg.frostedresearch.knowledge.IdeaRecord;
import com.teammoeg.frostedresearch.knowledge.IdeaCandidate;
import com.teammoeg.frostedresearch.knowledge.KnowledgeOffer;
import com.teammoeg.frostedresearch.knowledge.KnowledgeRecord;
import com.teammoeg.frostedresearch.knowledge.ProtocolHandler;
import com.teammoeg.frostedresearch.knowledge.ResearchResult;
import com.teammoeg.frostedresearch.knowledge.ResearchResultCatalog;
import com.teammoeg.frostedresearch.knowledge.ResearchTopicDefinition;
import com.teammoeg.frostedresearch.knowledge.ResearchWorkflowRegistry;
import com.teammoeg.frostedresearch.network.FHKnowledgeDataSyncPacket;
import com.teammoeg.frostedheart.content.utility.oredetect.GeologyResearchIntegration;
import com.teammoeg.frostedheart.content.utility.oredetect.OreProspectingModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/** Sole mutation service for Phase 1 team results and prototype fabrication. */
public final class TeamResearchService {
    /** Compatibility aliases; executable geology behavior lives in the Frosted Heart integration. */
    public static final ResourceLocation ROCK_TOPIC = GeologyResearchIntegration.TOPIC;
    public static final ResourceLocation ROCK_IDEA = GeologyResearchIntegration.IDEA;
    public static final ResourceLocation ROCK_FINDING = GeologyResearchIntegration.FINDING;
    public static final ResourceLocation COPPER_PICK_DESIGN = GeologyResearchIntegration.COPPER_PICK_DESIGN;
    private TeamResearchService() {
    }

    public static boolean archiveObservation(ServerPlayer player, KnowledgeRecord record) {
        TeamDataClosure<TeamKnowledgeData> closure = KnowledgeDataAPI.getData(player);
        boolean changed = closure.get().archiveObservation(record);
        if (changed) sync(closure.team());
        return changed;
    }

    public static boolean recordIdea(ServerPlayer player, ResourceLocation topicId, ResourceLocation ideaId,
            String source, Set<UUID> evidence) {
        TeamDataClosure<TeamKnowledgeData> closure = KnowledgeDataAPI.getData(player);
        IdeaRecord candidate = IdeaRecord.create(topicId, ideaId, source, evidence,
                player.serverLevel().getGameTime());
        ResearchTopicDefinition topic = ResearchResultCatalog.current().topics().get(topicId);
        if (topic == null || !declaresIdea(topic, ideaId)) {
            candidate = candidate.withState(IdeaRecord.State.ORPHAN,
                    player.serverLevel().getGameTime());
        }
        boolean changed = closure.get().recordIdea(candidate);
        if (changed) sync(closure.team());
        return changed;
    }

    public static boolean acceptKnowledgeOffer(ServerPlayer player, KnowledgeOffer offer) {
        return recordIdea(player, offer.topicId(), offer.ideaId(), offer.source(), Set.of());
    }

    public static List<IdeaCandidate> findIdeaCandidates(TeamKnowledgeData data, Set<UUID> evidence) {
        return ResearchWorkflowRegistry.findCandidates(data, evidence);
    }

    public static List<ActionCard> actionCards(TeamKnowledgeData data) {
        return ResearchWorkflowRegistry.actionCards(data);
    }

    /** Compatibility overload retained while callers move to the all-open-ideas projection. */
    public static List<ActionCard> actionCards(TeamKnowledgeData data, ResourceLocation topicId) {
        return actionCards(data).stream().filter(card -> card.topicId().equals(topicId)).toList();
    }

    public static Optional<FieldComparisonArtifact> executeProtocol(ServerPlayer player,
            ResourceLocation topicId, ResourceLocation protocolId) {
        return executeProtocolInternal(player, topicId, protocolId)
                .flatMap(ProtocolHandler.Execution::artifact);
    }

    /** Executes a registered method even when that method produces no comparison artifact. */
    public static boolean executeProtocolAction(ServerPlayer player,
            ResourceLocation topicId, ResourceLocation protocolId) {
        return executeProtocolInternal(player, topicId, protocolId).isPresent();
    }

    private static Optional<ProtocolHandler.Execution> executeProtocolInternal(ServerPlayer player,
            ResourceLocation topicId, ResourceLocation protocolId) {
        TeamDataClosure<TeamKnowledgeData> closure = KnowledgeDataAPI.getData(player);
        TeamKnowledgeData data = closure.get();
        ResearchTopicDefinition topic = ResearchResultCatalog.current().topics().get(topicId);
        if (topic == null) return Optional.empty();
        ResearchTopicDefinition.Protocol protocol = topic.protocols().stream()
                .filter(value -> value.id().equals(protocolId)).findFirst().orElse(null);
        if (protocol == null) return Optional.empty();
        ProtocolHandler handler = ResearchWorkflowRegistry.protocol(protocol.resolver());
        if (handler == null) return Optional.empty();
        IdeaRecord idea = data.ideas().stream().filter(value -> value.topicId().equals(topicId))
                .filter(value -> value.state() != IdeaRecord.State.RESOLVED
                        && value.state() != IdeaRecord.State.ORPHAN).findFirst().orElse(null);
        if (idea == null) return Optional.empty();
        Optional<ProtocolHandler.Execution> result = handler.execute(player, topicId, topic, protocol, data, idea);
        if (result.isEmpty()) return Optional.empty();
        boolean changed = result.get().artifact().map(data::appendComparison).orElse(false);
        IdeaRecord updatedIdea = result.get().attachedEvidence().isEmpty() ? idea
                : idea.withEvidence(result.get().attachedEvidence(), player.serverLevel().getGameTime());
        if (result.get().ideaReady()) {
            updatedIdea = updatedIdea.withState(IdeaRecord.State.READY,
                    player.serverLevel().getGameTime());
        }
        changed |= data.updateIdea(updatedIdea);
        if (changed) sync(closure.team());
        return changed ? result : Optional.empty();
    }

    public static Optional<FieldComparisonArtifact> executeNextProtocol(ServerPlayer player) {
        TeamKnowledgeData data = KnowledgeDataAPI.getData(player).get();
        for (ActionCard action : actionCards(data)) {
            Optional<FieldComparisonArtifact> result = executeProtocol(player, action.topicId(), action.protocolId());
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    public static Optional<FieldComparisonArtifact> compareRockSamples(ServerPlayer player) {
        return executeProtocol(player, ROCK_TOPIC, new ResourceLocation("frostedheart", "compare_rock_samples"));
    }

    public static boolean acceptTopicResults(ServerPlayer player, ResourceLocation topicId) {
        TeamDataClosure<TeamKnowledgeData> closure = KnowledgeDataAPI.getData(player);
        TeamKnowledgeData data = closure.get();
        ResearchTopicDefinition topicDefinition = ResearchResultCatalog.current().topics().get(topicId);
        if (topicDefinition == null || topicDefinition.resolution().isEmpty()) return false;
        ResearchTopicDefinition.Resolution resolution = topicDefinition.resolution().get();
        Optional<IdeaRecord> idea = data.idea(topicId, resolution.idea());
        com.teammoeg.frostedresearch.knowledge.ResolutionHandler resolver =
                ResearchWorkflowRegistry.resolution(resolution.resolver());
        if (idea.isEmpty() || resolver == null
                || !resolver.canResolve(topicId, topicDefinition, resolution, data, idea.get())) return false;
        ResearchTopicDefinitionAccess topic = topicResults(topicId);
        if (topic.results().isEmpty()) return false;
        boolean changed = false;
        for (ResearchResult result : topic.results()) {
            if (result instanceof ResearchResult.Finding) changed |= data.acquireFinding(result.id());
            else if (result instanceof ResearchResult.Design) changed |= data.acquireDesign(result.id());
            else if (result instanceof ResearchResult.Construction) changed |= data.acquireConstruction(result.id());
            else if (result instanceof ResearchResult.Procedure) changed |= data.acquireProcedure(result.id());
        }
        changed |= data.updateIdea(idea.get().withState(IdeaRecord.State.RESOLVED,
                player.serverLevel().getGameTime()));
        if (changed) sync(closure.team());
        return true;
    }

    public static boolean acceptNextReadyTopicResults(ServerPlayer player) {
        TeamKnowledgeData data = KnowledgeDataAPI.getData(player).get();
        return data.ideas().stream().filter(idea -> idea.state() == IdeaRecord.State.READY)
                .map(IdeaRecord::topicId).distinct().anyMatch(topic -> acceptTopicResults(player, topic));
    }

    private static ResearchTopicDefinitionAccess topicResults(ResourceLocation topicId) {
        com.teammoeg.frostedresearch.knowledge.ResearchTopicDefinition topic =
                ResearchResultCatalog.current().topics().get(topicId);
        if (topic == null || topic.resolution().isEmpty()) return new ResearchTopicDefinitionAccess(List.of());
        Set<ResourceLocation> resolvedIds = Set.copyOf(topic.resolution().get().results());
        return new ResearchTopicDefinitionAccess(topic.results().stream()
                .filter(result -> resolvedIds.contains(result.id())).toList());
    }

    private static boolean declaresIdea(ResearchTopicDefinition topic, ResourceLocation ideaId) {
        return topic.ideaSources().stream().anyMatch(source -> source.idea().equals(ideaId))
                || topic.inspiration().map(value -> value.idea().equals(ideaId)).orElse(false)
                || topic.resolution().map(value -> value.idea().equals(ideaId)).orElse(false);
    }

    public static EvidenceSelection selectRockEvidence(TeamKnowledgeData data) {
        return selectRockEvidence(data, data.observations().stream().map(KnowledgeRecord::id)
                .collect(java.util.stream.Collectors.toSet()));
    }

    public static EvidenceSelection selectRockEvidence(TeamKnowledgeData data, Set<UUID> allowedIds) {
        GeologyResearchIntegration.EvidenceSelection selection =
                GeologyResearchIntegration.selectEvidence(data, allowedIds, true);
        return new EvidenceSelection(selection.outcrop(), selection.nearby(), selection.control());
    }

    public static FieldComparisonArtifact.Outcome compareSamples(KnowledgeRecord nearby, KnowledgeRecord control) {
        return GeologyResearchIntegration.compareSamples(nearby, control);
    }

    public static FieldComparisonArtifact.Outcome compareSignals(Boolean nearbyCopper, Boolean controlCopper) {
        return GeologyResearchIntegration.compareSignals(nearbyCopper, controlCopper);
    }

    public static boolean hasCopper(OreProspectingModel.Snapshot snapshot) {
        return GeologyResearchIntegration.hasCopper(snapshot);
    }

    public record EvidenceSelection(Optional<KnowledgeRecord> outcrop,
            Optional<KnowledgeRecord> nearby, Optional<KnowledgeRecord> control) {
        public Set<UUID> ids() {
            Set<UUID> ids = new LinkedHashSet<>();
            outcrop.ifPresent(record -> ids.add(record.id()));
            nearby.ifPresent(record -> ids.add(record.id()));
            control.ifPresent(record -> ids.add(record.id()));
            return Set.copyOf(ids);
        }
    }

    private record ResearchTopicDefinitionAccess(List<ResearchResult> results) {
    }

    public static GrantResult grantResult(ServerPlayer player, ResourceLocation resultId) {
        ResearchResultCatalog.ResultEntry entry = ResearchResultCatalog.current().result(resultId);
        if (entry == null) return new GrantResult(Status.UNKNOWN_RESULT, resultId, null);
        TeamDataClosure<TeamKnowledgeData> closure = KnowledgeDataAPI.getData(player);
        ResearchResult result = entry.result();
        if (result instanceof ResearchResult.Prototype prototype) {
            PrototypeProfileDefinition profile = ResearchResultCatalog.current().profile(prototype.profile());
            if (profile == null) return new GrantResult(Status.UNKNOWN_PROFILE, resultId, null);
            UpgradePrototypeItem item = FRContents.Items.UPGRADE_PROTOTYPE.get();
            ItemStack stack = item.create(prototype.profile(), profile.revision(), closure.team().getId());
            ItemHandlerHelper.giveItemToPlayer(player, stack);
            return new GrantResult(Status.FABRICATED, resultId,
                    UpgradePrototypeItem.identity(stack).orElse(null));
        }

        boolean changed;
        if (result instanceof ResearchResult.Finding) {
            changed = closure.get().acquireFinding(resultId);
        } else if (result instanceof ResearchResult.Design) {
            changed = closure.get().acquireDesign(resultId);
        } else if (result instanceof ResearchResult.Construction) {
            changed = closure.get().acquireConstruction(resultId);
        } else if (result instanceof ResearchResult.Procedure) {
            changed = closure.get().acquireProcedure(resultId);
        } else {
            throw new IllegalStateException("Unhandled result type " + result.type());
        }
        // An idempotent administrator action also reconciles a potentially stale
        // client projection (for example after a resource reload).
        sync(closure.team());
        return new GrantResult(changed ? Status.ACQUIRED : Status.ALREADY_ACQUIRED, resultId, null);
    }

    /** Revokes team-owned results, including orphan IDs no longer present in the catalogue. */
    public static RevokeResult revokeResult(ServerPlayer player, ResourceLocation resultId) {
        ResearchResultCatalog.ResultEntry entry = ResearchResultCatalog.current().result(resultId);
        if (entry != null && entry.result() instanceof ResearchResult.Prototype) {
            return new RevokeResult(Status.PHYSICAL_RESULT_NOT_REVOCABLE, resultId, Set.of());
        }

        TeamDataClosure<TeamKnowledgeData> closure = KnowledgeDataAPI.getData(player);
        TeamKnowledgeData data = closure.get();
        Set<ResearchResult.ResultType> revokedTypes = new LinkedHashSet<>();
        if (data.revokeFinding(resultId)) revokedTypes.add(ResearchResult.ResultType.FINDING);
        if (data.revokeDesign(resultId)) revokedTypes.add(ResearchResult.ResultType.DESIGN);
        if (data.revokeConstruction(resultId)) revokedTypes.add(ResearchResult.ResultType.CONSTRUCTION);
        if (data.revokeProcedure(resultId)) revokedTypes.add(ResearchResult.ResultType.PROCEDURE);
        if (!revokedTypes.isEmpty()) {
            sync(closure.team());
            return new RevokeResult(Status.REVOKED, resultId, revokedTypes);
        }
        if (entry != null) sync(closure.team());
        return new RevokeResult(entry == null ? Status.UNKNOWN_RESULT : Status.NOT_ACQUIRED,
                resultId, Set.of());
    }

    /** Describes both current catalogue definitions and retained orphan acquisition state. */
    public static ResultInfo resultInfo(ServerPlayer player, ResourceLocation resultId) {
        TeamDataClosure<TeamKnowledgeData> closure = KnowledgeDataAPI.getData(player);
        TeamKnowledgeData data = closure.get();
        ResearchResultCatalog.Snapshot catalog = ResearchResultCatalog.current();
        ResearchResultCatalog.ResultEntry entry = catalog.result(resultId);
        OptionalInt profileRevision = OptionalInt.empty();
        if (entry != null && entry.result() instanceof ResearchResult.Prototype prototype) {
            PrototypeProfileDefinition profile = catalog.profile(prototype.profile());
            if (profile != null) profileRevision = OptionalInt.of(profile.revision());
        }
        return new ResultInfo(closure.team().getId(), resultId,
                Optional.ofNullable(entry).map(ResearchResultCatalog.ResultEntry::topicId),
                Optional.ofNullable(entry).map(ResearchResultCatalog.ResultEntry::result),
                acquiredTypes(data, resultId), profileRevision);
    }

    private static Set<ResearchResult.ResultType> acquiredTypes(
            TeamKnowledgeData data, ResourceLocation resultId) {
        Set<ResearchResult.ResultType> types = new LinkedHashSet<>();
        if (data.hasFinding(resultId)) types.add(ResearchResult.ResultType.FINDING);
        if (data.hasDesign(resultId)) types.add(ResearchResult.ResultType.DESIGN);
        if (data.hasConstruction(resultId)) types.add(ResearchResult.ResultType.CONSTRUCTION);
        if (data.hasProcedure(resultId)) types.add(ResearchResult.ResultType.PROCEDURE);
        return Set.copyOf(types);
    }

    public static void sync(TeamDataHolder team) {
        FHKnowledgeDataSyncPacket snapshot = new FHKnowledgeDataSyncPacket(team);
        team.forEachOnline(player -> {
            // A player can already appear in team membership during login or a GameTest
            // before its network listener has been attached. The login event sends the
            // same full snapshot once that listener exists.
            if (player.connection != null) FRNetwork.INSTANCE.sendPlayer(player, snapshot);
        });
    }

    public enum Status {
        UNKNOWN_RESULT,
        UNKNOWN_PROFILE,
        ACQUIRED,
        ALREADY_ACQUIRED,
        FABRICATED,
        REVOKED,
        NOT_ACQUIRED,
        PHYSICAL_RESULT_NOT_REVOCABLE
    }

    public record GrantResult(Status status, ResourceLocation resultId,
            UpgradePrototypeItem.Identity prototype) {
        public boolean succeeded() {
            return status == Status.ACQUIRED || status == Status.ALREADY_ACQUIRED || status == Status.FABRICATED;
        }
    }

    public record RevokeResult(Status status, ResourceLocation resultId,
            Set<ResearchResult.ResultType> revokedTypes) {
        public RevokeResult {
            revokedTypes = Set.copyOf(revokedTypes);
        }

        public boolean succeeded() {
            return status == Status.REVOKED || status == Status.NOT_ACQUIRED;
        }
    }

    public record ResultInfo(UUID teamId, ResourceLocation resultId,
            Optional<ResourceLocation> topicId,
            Optional<ResearchResult> definition,
            Set<ResearchResult.ResultType> acquiredTypes,
            OptionalInt profileRevision) {
        public ResultInfo {
            acquiredTypes = Set.copyOf(acquiredTypes);
        }

        public boolean exists() {
            return definition.isPresent() || !acquiredTypes.isEmpty();
        }

        public boolean orphan() {
            return definition.isEmpty() && !acquiredTypes.isEmpty();
        }
    }
}

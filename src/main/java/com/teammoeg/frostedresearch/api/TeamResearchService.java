/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.api;

import com.teammoeg.chorda.dataholders.team.TeamDataClosure;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.FRNetwork;
import com.teammoeg.frostedresearch.data.TeamKnowledgeData;
import com.teammoeg.frostedresearch.item.UpgradePrototypeItem;
import com.teammoeg.frostedresearch.knowledge.PrototypeProfileDefinition;
import com.teammoeg.frostedresearch.knowledge.ResearchResult;
import com.teammoeg.frostedresearch.knowledge.ResearchResultCatalog;
import com.teammoeg.frostedresearch.knowledge.network.KnowledgeSnapshotPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.*;

/**
 * Sole mutation service for Phase 1 team results and prototype fabrication.
 */
public final class TeamResearchService {
    private TeamResearchService() {
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

        var learned = com.teammoeg.frostedresearch.knowledge.KnowledgeService.forPlayer(player).learn(
                com.teammoeg.frostedresearch.knowledge.model.KnowledgeElement.result(resultId),
                com.teammoeg.frostedresearch.knowledge.state.AcquisitionSource.of("command", player),
                com.teammoeg.frostedresearch.knowledge.KnowledgeService.Grant.COMMAND);
        sync(closure.team());
        return new GrantResult(learned.succeeded() ? Status.ACQUIRED
                : learned.status() == com.teammoeg.frostedresearch.knowledge.KnowledgeService.Status.ALREADY_OWNED
                ? Status.ALREADY_ACQUIRED : Status.REJECTED, resultId, null);
    }

    /**
     * Revokes team-owned results, including orphan IDs no longer present in the catalogue.
     */
    public static RevokeResult revokeResult(ServerPlayer player, ResourceLocation resultId) {
        ResearchResultCatalog.ResultEntry entry = ResearchResultCatalog.current().result(resultId);
        if (entry != null && entry.result() instanceof ResearchResult.Prototype) {
            return new RevokeResult(Status.PHYSICAL_RESULT_NOT_REVOCABLE, resultId, Set.of());
        }

        TeamDataClosure<TeamKnowledgeData> closure = KnowledgeDataAPI.getData(player);
        TeamKnowledgeData data = closure.get();
        Set<ResearchResult.ResultType> revokedTypes = acquiredTypes(data, resultId);
        var forgotten = com.teammoeg.frostedresearch.knowledge.KnowledgeService.forPlayer(player)
                .forget(com.teammoeg.frostedresearch.knowledge.model.KnowledgeKey.result(resultId));
        if (forgotten.succeeded()) return new RevokeResult(Status.REVOKED, resultId, revokedTypes);
        if (entry != null) sync(closure.team());
        return new RevokeResult(forgotten.status() == com.teammoeg.frostedresearch.knowledge.KnowledgeService.Status.CANCELLED
                ? Status.REJECTED : entry == null ? Status.UNKNOWN_RESULT : Status.NOT_ACQUIRED, resultId, Set.of());
    }

    /**
     * Describes both current catalogue definitions and retained orphan acquisition state.
     */
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
        var fragments = KnowledgeSnapshotPacket.create(team);
        team.forEachOnline(player -> {
            // A player can already appear in team membership during login or a GameTest
            // before its network listener has been attached. The login event sends the
            // same full snapshot once that listener exists.
            if (player.connection != null)
                for (var fragment : fragments) FRNetwork.INSTANCE.sendPlayer(player, fragment);
        });
    }

    public enum Status {
        REJECTED,
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

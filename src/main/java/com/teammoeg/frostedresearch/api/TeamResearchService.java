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
import com.teammoeg.frostedresearch.network.FHKnowledgeDataSyncPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/** Sole mutation service for Phase 1 team results and prototype fabrication. */
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

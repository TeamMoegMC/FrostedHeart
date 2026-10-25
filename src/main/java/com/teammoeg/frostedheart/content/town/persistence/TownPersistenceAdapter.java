package com.teammoeg.frostedheart.content.town.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.teammoeg.chorda.dataholders.DataHolderMap.InstallReason;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataType;
import com.teammoeg.chorda.dataholders.persistence.PersistenceCoordinator;
import com.teammoeg.chorda.dataholders.persistence.PersistenceCoordinator.CommitFacts;
import com.teammoeg.chorda.dataholders.persistence.PersistenceCoordinator.Kind;
import com.teammoeg.chorda.dataholders.persistence.PersistenceCoordinator.RecordKey;
import com.teammoeg.chorda.dataholders.persistence.PersistenceCoordinator.RecordSource;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.dataholders.team.TeamRecordExtension;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.ai_town.AITownData;
import com.teammoeg.frostedheart.content.town.ai_town.AITownManager;
import com.teammoeg.frostedheart.content.town.citizen.sim.TownSimData;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

/** Owns town record formats and migration, never a second copy of live town data. */
public final class TownPersistenceAdapter implements TeamRecordExtension {
    private static final RecordKey METADATA = new RecordKey(Kind.TOWN_METADATA, new UUID(0, 0));
    private final CTeamDataManager manager;
    private final PersistenceCoordinator persistence;
    private final Map<UUID, CompoundTag> originalAI = new HashMap<>();
    private CompoundTag metadataRoot = new CompoundTag();
    private CompoundTag legacyTowns = new CompoundTag();
    private CompoundTag legacyPlayerSims = new CompoundTag();
    private PersistenceCoordinator.RecordState metadataState;
    private IOException metadataLoadFailure;
    private final com.teammoeg.frostedheart.content.town.TownRuntimeManager runtime;

    public TownPersistenceAdapter(CTeamDataManager manager) {
        this.manager = manager;
        this.persistence = manager.persistence();
        this.runtime = new com.teammoeg.frostedheart.content.town.TownRuntimeManager(manager);
    }

    @Override
    public Set<String> ownedRootKeys() { return Set.of("townSim"); }

    @Override
    public void prepareLoad(CTeamDataManager ignored) {
        AITownManager.reset();
        Path metadataPath = manager.root().resolve("data/fh_ai_towns.dat");
        if (Files.exists(metadataPath)) {
            try {
                metadataRoot = NbtIo.readCompressed(metadataPath.toFile());
                CompoundTag data = metadataRoot.getCompound("data");
                legacyTowns = data.getCompound("towns").copy();
                legacyPlayerSims = data.getCompound("playerSims").copy();
                if (data.hasUUID("lastAIId")) AITownManager.restoreLastAIId(data.getUUID("lastAIId"));
                data.remove("towns");
                data.remove("playerSims");
            } catch (IOException ex) {
                metadataLoadFailure = ex;
                FHMain.LOGGER.error("Unable to load town migration metadata {}", metadataPath, ex);
            }
        }
        metadataState = persistence.registerRecord(METADATA, metadataPath, new RecordSource() {
            @Override public void prepareCheckpoint() {
                if (metadataLoadFailure != null) throw new IllegalStateException("Town migration source was not read", metadataLoadFailure);
            }
            @Override public CompoundTag capture() {
                if (metadataLoadFailure != null) throw new IllegalStateException("Town migration source was not read", metadataLoadFailure);
                CompoundTag root = metadataRoot.copy();
                root.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
                CompoundTag data = root.getCompound("data");
                data.put("towns", legacyTowns.copy());
                data.put("playerSims", legacyPlayerSims.copy());
                UUID last = AITownManager.lastAIId();
                if (last != null) data.putUUID("lastAIId", last);
                else data.remove("lastAIId");
                root.put("data", data);
                return root;
            }
        });
        AITownManager.bindPersistence(this::registerAI, this::bindPlayerSim, metadataState::markChanged);
        loadAI();
    }

    private void loadAI() {
        Set<UUID> currentIds = new HashSet<>();
        Path folder = manager.root().resolve("data/fh_ai_towns");
        if (Files.isDirectory(folder)) {
            try (var paths = Files.newDirectoryStream(folder, "*.nbt")) {
                for (Path path : paths) {
                    try {
                        CompoundTag root = NbtIo.readCompressed(path.toFile());
                        UUID id = root.getUUID("id");
                        currentIds.add(id);
                        if (root.getBoolean("deleted")) {
                            if (legacyTowns.contains(id.toString())) {
                                legacyTowns.remove(id.toString());
                                metadataState.markChanged();
                            }
                            continue;
                        }
                        AITownData decoded = AITownData.fromNbt(root.getCompound("data"));
                        for (String field : java.util.List.of("id", "name", "dimension", "residents", "sim")) root.getCompound("data").remove(field);
                        originalAI.put(id, root);
                        AITownManager.restoreAI(decoded);
                    } catch (Exception ex) {
                        FHMain.LOGGER.error("Unable to restore AI town {}", path, ex);
                    }
                }
            } catch (IOException ex) {
                FHMain.LOGGER.error("Unable to enumerate AI towns {}", folder, ex);
            }
        }
        for (String key : legacyTowns.getAllKeys()) {
            try {
                if (!currentIds.contains(UUID.fromString(key))) {
                    CompoundTag payload = legacyTowns.getCompound(key);
                    AITownData decoded = AITownData.fromNbt(payload);
                    CompoundTag original = new CompoundTag();
                    CompoundTag unknown = new CompoundTag();
                    Set<String> known = Set.of("id", "name", "dimension", "residents", "sim");
                    for (String field : payload.getAllKeys()) if (!known.contains(field)) unknown.put(field, payload.get(field).copy());
                    original.put("data", unknown);
                    originalAI.put(decoded.getId(), original);
                    AITownManager.restoreAI(decoded);
                }
            } catch (Exception ex) {
                FHMain.LOGGER.error("Unable to restore legacy AI town {}", key, ex);
            }
        }
    }

    private void registerAI(AITownData town) {
        var state = persistence.registerRecord(new RecordKey(Kind.AI_TOWN, town.getId()),
                manager.root().resolve("data/fh_ai_towns").resolve(town.getId() + ".nbt"), new RecordSource() {
                    @Override public void prepareCheckpoint() {}
                    @Override public CompoundTag capture() {
                        CompoundTag root = originalAI.getOrDefault(town.getId(), new CompoundTag()).copy();
                        root.putUUID("id", town.getId());
                        root.remove("deleted");
                        CompoundTag data = root.getCompound("data");
                        CompoundTag current = town.capturePersistenceData();
                        for (String key : current.getAllKeys()) data.put(key, current.get(key));
                        root.put("data", data);
                        return root;
                    }
                });
        town.setPersistenceListener(state::markChanged);
    }

    private void bindPlayerSim(UUID holderId, TownSimData sim) {
        TeamDataHolder holder = manager.findByHolderId(holderId);
        if (holder == null) return;
        sim.setMarkDirty(holder::markPersistentChanged);
        holder.markPersistentChanged();
    }

    @Override
    public void loaded(TeamDataHolder holder, CompoundTag root, boolean legacy) {
        if (holder.getOptional(FHSpecialDataTypes.TOWN_DATA).isEmpty()) return;
        CompoundTag simTag = legacy ? legacyPlayerSims.getCompound(holder.getId().toString()) : root.getCompound("townSim");
        if (!simTag.isEmpty()) {
            TownSimData sim = new TownSimData();
            sim.loadFromNbt(simTag);
            AITownManager.restorePlayerSim(holder.getId(), sim);
        }
    }

    @Override
    public void afterLoad() {
        manager.getAllData().forEach(runtime::install);
    }

    @Override
    public void created(TeamDataHolder holder) { runtime.install(holder); }

    @Override
    public boolean deferIfBusy(TeamDataHolder holder, Runnable request) { return runtime.deferIfBusy(holder, request); }

    @Override
    public void capturePart(TeamDataHolder holder, CompoundTag root) {
        TownSimData sim = AITownManager.peekPlayerSim(holder.getId());
        root.put("townSim", holder.getOptional(FHSpecialDataTypes.TOWN_DATA).isEmpty() || sim == null
                ? new CompoundTag() : sim.capturePersistencePart());
    }

    @Override
    public void componentInstalled(TeamDataHolder holder, SpecialDataType<?> type,
                                   SpecialData previous, SpecialData next, InstallReason reason) {
        if (previous instanceof com.teammoeg.frostedresearch.data.TeamResearchData research) research.detachResearch(holder);
        if (next instanceof com.teammoeg.frostedresearch.data.TeamResearchData research) research.initResearch(holder);
        if ("frostedresearch:knowledge".equals(type.getId()) && next != null && reason != InstallReason.CREATE) {
            com.teammoeg.frostedresearch.knowledge.KnowledgeService.forTeam(holder, 0).initialize();
        }
        if (type == FHSpecialDataTypes.TOWN_DATA || type == FHSpecialDataTypes.GENERATOR_DATA) {
            if (previous != null && reason != InstallReason.RESTORE) {
                holder.forEachOnline(player -> { if (player.containerMenu != player.inventoryMenu) player.closeContainer(); });
            }
            runtime.install(holder, reason == InstallReason.RESTORE);
            if (type == FHSpecialDataTypes.TOWN_DATA && next == null) AITownManager.removePlayerSim(holder.getId());
            com.teammoeg.frostedheart.content.town.TownSync.invalidate(holder);
        }
    }

    @Override
    public void teamChanged(TeamDataHolder holder) { com.teammoeg.frostedheart.content.town.TownSync.invalidate(holder); }

    @Override
    public void retired(TeamDataHolder holder) {
        holder.getOptional(com.teammoeg.frostedresearch.FRSpecialDataTypes.RESEARCH_DATA)
                .ifPresent(data -> data.detachResearch(holder));
        runtime.detach(holder);
        AITownManager.removePlayerSim(holder.getId());
    }

    @Override
    public void committed(RecordKey key, CommitFacts facts) {
        if (facts.storageVersion() < 1) return;
        CompoundTag source = key.kind() == Kind.TEAM ? legacyPlayerSims
                : key.kind() == Kind.AI_TOWN ? legacyTowns : null;
        if (source != null && source.contains(key.id().toString())) {
            source.remove(key.id().toString());
            metadataState.markChanged();
        }
    }

    @Override
    public void stopping() { runtime.stopping(); }

    @Override
    public void reset() {
        runtime.close();
        AITownManager.reset();
        originalAI.clear();
    }
}

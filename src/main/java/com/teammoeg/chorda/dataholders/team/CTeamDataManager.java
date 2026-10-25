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

package com.teammoeg.chorda.dataholders.team;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataType;
import com.teammoeg.chorda.dataholders.persistence.NbtRecordWriter;
import com.teammoeg.chorda.dataholders.persistence.PersistenceCoordinator;
import com.teammoeg.chorda.dataholders.persistence.PersistenceCoordinator.CommitFacts;
import com.teammoeg.chorda.dataholders.persistence.PersistenceCoordinator.Kind;
import com.teammoeg.chorda.dataholders.persistence.PersistenceCoordinator.RecordKey;
import com.teammoeg.chorda.dataholders.persistence.PersistenceCoordinator.RecordSource;
import com.teammoeg.chorda.events.TeamCreatedEvent;
import com.teammoeg.chorda.util.CDistHelper;

import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;

/** Server-session owner of team identities, component records and save completion. */
public class CTeamDataManager {
    public static CTeamDataManager INSTANCE;
    private static Function<CTeamDataManager, TeamRecordExtension> extensionFactory = manager -> TeamRecordExtension.NONE;

    private final MinecraftServer server;
    private final Path root;
    private final PersistenceCoordinator persistence;
    private final TeamRecordExtension extension;
    private final Map<UUID, UUID> dataByFTBId = new HashMap<>();
    private final Map<UUID, TeamDataHolder> dataByOwnId = new LinkedHashMap<>();
    private final Map<UUID, List<Path>> legacyAliases = new HashMap<>();
    private boolean stopping;

    public CTeamDataManager() {
        this(CDistHelper.getServer());
    }

    public CTeamDataManager(MinecraftServer server) {
        this.server = server;
        root = server.getWorldPath(LevelResource.ROOT);
        persistence = new PersistenceCoordinator(Util.ioPool(), new NbtRecordWriter());
        extension = extensionFactory.apply(this);
        persistence.setCommitListener(this::committed);
        INSTANCE = this;
    }

    public static void registerExtension(Function<CTeamDataManager, TeamRecordExtension> factory) {
        extensionFactory = factory;
    }

    public MinecraftServer server() { return server; }
    public Path root() { return root; }
    public PersistenceCoordinator persistence() { return persistence; }
    public boolean isStopping() { return stopping; }

    public Collection<TeamDataHolder> getAllData() {
        return Collections.unmodifiableCollection(dataByOwnId.values());
    }

    public <T extends SpecialData> Stream<T> getAllData(SpecialDataType<T> type) {
        return dataByOwnId.values().stream().flatMap(holder -> holder.getOptional(type).stream());
    }

    public <T extends SpecialData> void forAllData(SpecialDataType<T> type, BiConsumer<T, TeamDataHolder> consumer) {
        for (TeamDataHolder holder : new ArrayList<>(dataByOwnId.values())) {
            holder.getOptional(type).ifPresent(data -> consumer.accept(data, holder));
        }
    }

    @Nullable
    public static TeamDataHolder getDataByResearchID(UUID id) {
        if (INSTANCE == null) return new TeamDataHolder(UUID.randomUUID(), new ClientTeam());
        return INSTANCE.get(id);
    }

    public static TeamDataHolder get(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || INSTANCE == null) return null;
        AbstractTeam team = TeamsAPI.getAPI().getTeamByPlayer(serverPlayer);
        return team == null ? null : INSTANCE.get(team);
    }

    public TeamDataHolder tryLoad(TeamDataHolder holder) {
        if (holder != null) holder.loadIfNeeded();
        return holder;
    }

    public TeamDataHolder get(AbstractTeam team) {
        TeamDataHolder holder = findByExternalTeamId(team.getId());
        if (holder == null) {
            holder = new TeamDataHolder(UUID.randomUUID(), team);
            register(holder);
            dataByOwnId.put(holder.getId(), holder);
            dataByFTBId.put(team.getId(), holder.getId());
            extension.created(holder);
            MinecraftForge.EVENT_BUS.post(new TeamCreatedEvent(holder));
        } else {
            holder.setTeam(team);
            tryLoad(holder);
        }
        if (holder.getOwnerName() == null) updateOwnerName(holder, team);
        return holder;
    }

    @Nullable
    public TeamDataHolder get(UUID id) { return tryLoad(findByHolderId(id)); }

    /** Pure lookups never create components or post loading events. */
    @Nullable
    public TeamDataHolder findByHolderId(UUID holderId) { return dataByOwnId.get(holderId); }

    @Nullable
    public TeamDataHolder findByExternalTeamId(UUID externalTeamId) {
        return dataByOwnId.get(dataByFTBId.get(externalTeamId));
    }

    private void register(TeamDataHolder holder) {
        var state = persistence.registerRecord(new RecordKey(Kind.TEAM, holder.getId()),
                root.resolve("chorda_data").resolve(holder.getId() + ".nbt"), new RecordSource() {
                    @Override public void prepareCheckpoint() { holder.prepareCheckpoint(); }
                    @Override public CompoundTag capture() { return holder.captureRecord(); }
                });
        holder.bindServer(state, extension);
    }

    private record StoredTeam(Path path, UUID id, UUID externalId, CompoundTag root, boolean canonical) {}

    /** Read all durable exclusion facts before constructing any external-team map. */
    public void load() {
        extension.prepareLoad(this);
        Path folder = root.resolve("chorda_data");
        List<StoredTeam> stored = new ArrayList<>();
        Set<UUID> excluded = new HashSet<>();
        try {
            Files.createDirectories(folder);
            try (var files = Files.newDirectoryStream(folder, "*.nbt")) {
                for (Path path : files) {
                    try {
                        CompoundTag tag = NbtIo.readCompressed(path.toFile());
                        UUID id = tag.getUUID("uuid");
                        UUID externalId = tag.hasUUID("teamId") ? tag.getUUID("teamId")
                                : UUID.fromString(path.getFileName().toString().replaceFirst("\\.nbt$", ""));
                        boolean canonical = path.getFileName().toString().equals(id + ".nbt");
                        stored.add(new StoredTeam(path, id, externalId, tag, canonical));
                        if (canonical && tag.getInt("storageVersion") >= 1) {
                            CommitFacts facts = facts(tag);
                            persistence.confirmLoaded(facts);
                            excluded.addAll(facts.supersededHolderIds());
                            if (facts.deleted()) excluded.add(id);
                        }
                    } catch (Exception ex) {
                        Chorda.LOGGER.error("Unable to read team record {}", path, ex);
                    }
                }
            }
        } catch (IOException ex) {
            Chorda.LOGGER.error("Unable to enumerate team records {}", folder, ex);
        }
        stored.sort(Comparator.comparing(StoredTeam::canonical).reversed()
                .thenComparing(record -> record.path().toString()));
        for (StoredTeam record : stored) {
            if (!record.canonical()) legacyAliases.computeIfAbsent(record.id(), ignored -> new ArrayList<>()).add(record.path());
            if (excluded.contains(record.id()) || dataByOwnId.containsKey(record.id())) continue;
            TeamDataHolder holder = new TeamDataHolder(record.id(), resolveExternalTeam(record.externalId()));
            holder.rememberExternalTeamId(record.externalId());
            try {
                register(holder);
                holder.deserialize(record.root(), false);
                dataByOwnId.put(holder.getId(), holder);
                dataByFTBId.put(record.externalId(), holder.getId());
                extension.loaded(holder, record.root(), record.root().getInt("storageVersion") < 1);
            } catch (Exception ex) {
                persistence.discardUnloaded(new RecordKey(Kind.TEAM, holder.getId()));
                holder.unbindServer();
                dataByOwnId.remove(holder.getId());
                dataByFTBId.remove(record.externalId(), holder.getId());
                Chorda.LOGGER.error("Unable to restore team record {}", record.path(), ex);
            }
        }
        extension.afterLoad();
    }

    private static CommitFacts facts(CompoundTag root) {
        Set<UUID> superseded = new HashSet<>();
        for (Tag tag : root.getList("supersededHolderIds", Tag.TAG_INT_ARRAY)) superseded.add(NbtUtils.loadUUID(tag));
        return new CommitFacts(root.getInt("storageVersion"), root.getBoolean("deleted"), superseded);
    }

    private AbstractTeam resolveExternalTeam(UUID externalId) {
        try { return TeamsAPI.getAPI().getTeamByUuid(externalId); }
        catch (RuntimeException failure) {
            Chorda.LOGGER.warn("External team {} is temporarily unavailable; preserving its internal record", externalId, failure);
            return null;
        }
    }

    private void committed(RecordKey key, CommitFacts facts) {
        extension.committed(key, facts);
        if (key.kind() != Kind.TEAM || facts.storageVersion() < 1) return;
        List<Path> aliases = legacyAliases.get(key.id());
        if (aliases == null) return;
        for (var iterator = aliases.iterator(); iterator.hasNext();) {
            Path alias = iterator.next();
            try {
                Files.deleteIfExists(alias);
                iterator.remove();
            } catch (IOException ex) {
                Chorda.LOGGER.warn("Could not remove migrated team alias {}", alias, ex);
            }
        }
        if (aliases.isEmpty()) legacyAliases.remove(key.id());
    }

    public void checkpoint() { persistence.checkpoint(server.getTickCount()); }

    /** Compatibility entry for explicit saves; completion means actual writes. */
    public void save() { saveAndAwait(); }

    public boolean saveAndAwait() {
        var report = persistence.checkpointAndAwait(server.getTickCount());
        if (!report.successful()) Chorda.LOGGER.error("Team/town save incomplete: {}", report.failures());
        return report.successful();
    }

    public void applyCompleted() {
        persistence.applyCompleted();
        persistence.applyCommitEffects();
    }

    public void prepareStopping() {
        stopping = true;
        extension.stopping();
        checkpoint();
    }

    public void close() {
        var report = persistence.close();
        if (!report.successful()) Chorda.LOGGER.error("Team/town stop checkpoint incomplete: {}", report.failures());
        extension.reset();
        for (TeamDataHolder holder : dataByOwnId.values()) holder.unbindServer();
        dataByOwnId.clear();
        dataByFTBId.clear();
    }

    public void transfer(UUID originalExternalId, AbstractTeam team) {
        UUID holderId = dataByFTBId.get(originalExternalId);
        if (holderId != null && dataByOwnId.containsKey(holderId)) transferByRid(holderId, team);
        else get(team);
    }

    public void transferByRid(UUID holderId, AbstractTeam team) {
        TeamDataHolder kept = dataByOwnId.get(holderId);
        if (kept == null) return;
        if (extension.deferIfBusy(kept, () -> transferByRid(holderId, team))) return;
        TeamDataHolder replaced = findByExternalTeamId(team.getId());
        if (replaced != null && replaced != kept) {
            kept.supersede(replaced);
            retire(replaced, true);
        }
        dataByFTBId.values().removeIf(holderId::equals);
        dataByFTBId.put(team.getId(), holderId);
        kept.setTeam(team);
        updateOwnerName(kept, team);
        extension.teamChanged(kept);
    }

    private void updateOwnerName(TeamDataHolder holder, AbstractTeam team) {
        if (server.getProfileCache() != null) {
            server.getProfileCache().get(team.getOwner()).ifPresent(profile -> holder.setOwnerName(profile.getName()));
        }
    }

    public void retire(TeamDataHolder holder, boolean needsReplacementConfirmation) {
        CompoundTag deletion = holder.deletionRecord();
        persistence.retire(holder.recordState(), new RecordSource() {
            @Override public void prepareCheckpoint() {}
            @Override public CompoundTag capture() { return deletion.copy(); }
        }, needsReplacementConfirmation);
        dataByOwnId.remove(holder.getId(), holder);
        dataByFTBId.values().removeIf(holder.getId()::equals);
        extension.retired(holder);
        holder.unbindServer();
    }
}

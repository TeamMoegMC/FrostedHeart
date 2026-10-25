package com.teammoeg.chorda.dataholders.team;

import com.teammoeg.chorda.dataholders.DataHolderMap.InstallReason;
import com.teammoeg.chorda.dataholders.SpecialData;
import com.teammoeg.chorda.dataholders.SpecialDataType;
import com.teammoeg.chorda.dataholders.persistence.PersistenceCoordinator;
import net.minecraft.nbt.CompoundTag;

/** Per-server bridge for mod-owned record fields and component consumers. */
public interface TeamRecordExtension {
    TeamRecordExtension NONE = new TeamRecordExtension() {};
    default java.util.Set<String> ownedRootKeys() { return java.util.Set.of(); }

    default void prepareLoad(CTeamDataManager manager) {}
    default void afterLoad() {}
    default void loaded(TeamDataHolder holder, CompoundTag root, boolean legacy) {}
    default void created(TeamDataHolder holder) {}
    default void capturePart(TeamDataHolder holder, CompoundTag root) {}
    default void componentInstalled(TeamDataHolder holder, SpecialDataType<?> type,
                                    SpecialData previous, SpecialData next, InstallReason reason) {}
    default boolean deferIfBusy(TeamDataHolder holder, Runnable request) { return false; }
    default void teamChanged(TeamDataHolder holder) {}
    default void retired(TeamDataHolder holder) {}
    default void committed(PersistenceCoordinator.RecordKey key, PersistenceCoordinator.CommitFacts facts) {}
    default void stopping() {}
    default void reset() {}
}

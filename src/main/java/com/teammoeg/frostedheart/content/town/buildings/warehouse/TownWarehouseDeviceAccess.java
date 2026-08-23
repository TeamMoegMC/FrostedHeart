/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.provider.TeamTownProvider;
import com.teammoeg.frostedheart.content.town.provider.ITownProviderSerializable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Shared server authority checks for town-owned warehouse automation devices. */
final class TownWarehouseDeviceAccess {
    private static final double MAX_DISTANCE_SQUARED = 64.0;

    private TownWarehouseDeviceAccess() {
    }

    static ClaimResult claimOrAuthorize(
            ServerPlayer player,
            BlockEntity blockEntity,
            TeamTownProvider currentProvider
    ) {
        if (!isLiveAndNear(player, blockEntity)) {
            return ClaimResult.DENIED;
        }
        TeamDataHolder holder = CTeamDataManager.get(player);
        ResourceKey<Level> townDimension = TeamTownData.resolveTownDimension(holder);
        if (holder == null || townDimension == null
                || !townDimension.equals(player.serverLevel().dimension())) {
            return ClaimResult.DENIED;
        }
        TeamTownProvider provider = currentProvider;
        if (provider == null) {
            provider = new TeamTownProvider(holder.getId());
        } else if (!Objects.equals(provider.ownerUUID, holder.getId())) {
            return ClaimResult.DENIED;
        }
        return new ClaimResult(true, provider, Optional.ofNullable(provider.getTown()));
    }

    static boolean isMenuAccessValid(
            Player player,
            BlockEntity blockEntity,
            TeamTownProvider provider,
            AbstractContainerMenu expectedMenu
    ) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || provider == null) {
            return false;
        }
        TeamDataHolder holder = CTeamDataManager.get(player);
        return menuAccessFactsMatch(
                player.containerMenu == expectedMenu,
                isLiveAndNear(serverPlayer, blockEntity),
                provider.ownerUUID,
                holder == null ? null : holder.getId(),
                TeamTownData.resolveTownDimension(holder),
                serverPlayer.serverLevel().dimension());
    }

    static Optional<TeamTown> resolveTown(TeamTownProvider provider, Level level) {
        if (provider == null || provider.ownerUUID == null || !(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        TeamDataHolder holder = CTeamDataManager.getDataByResearchID(provider.ownerUUID);
        if (holder == null || !serverLevel.dimension().equals(TeamTownData.resolveTownDimension(holder))) {
            return Optional.empty();
        }
        TeamTown town = provider.getTown();
        if (town == null) {
            return Optional.empty();
        }
        town.prepareWarehouseTopology(serverLevel.dimension());
        return Optional.of(town);
    }

    static boolean ownershipAndDimensionMatch(
            UUID ownerTeam,
            UUID playerTeam,
            ResourceKey<Level> townDimension,
            ResourceKey<Level> deviceDimension
    ) {
        return ownerTeam != null && ownerTeam.equals(playerTeam)
                && townDimension != null && townDimension.equals(deviceDimension);
    }

    static boolean menuAccessFactsMatch(
            boolean currentMenu,
            boolean liveAndNear,
            UUID ownerTeam,
            UUID playerTeam,
            ResourceKey<Level> townDimension,
            ResourceKey<Level> deviceDimension
    ) {
        return currentMenu && liveAndNear && ownershipAndDimensionMatch(
                ownerTeam, playerTeam, townDimension, deviceDimension);
    }

    static TeamTownProvider readProvider(CompoundTag nbt) {
        if (nbt == null || !nbt.contains("townProvider")) {
            return null;
        }
        return ITownProviderSerializable.fromNBT(nbt.getCompound("townProvider"))
                instanceof TeamTownProvider provider ? provider : null;
    }

    static void writeProvider(CompoundTag nbt, TeamTownProvider provider) {
        if (nbt != null && provider != null) {
            nbt.put("townProvider", provider.toNBT());
        }
    }

    private static boolean isLiveAndNear(ServerPlayer player, BlockEntity blockEntity) {
        return player != null && blockEntity != null && blockEntity.getLevel() == player.level()
                && !blockEntity.isRemoved()
                && blockEntity.getLevel().getBlockEntity(blockEntity.getBlockPos()) == blockEntity
                && player.distanceToSqr(
                blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5) <= MAX_DISTANCE_SQUARED;
    }

    record ClaimResult(boolean allowed, TeamTownProvider provider, Optional<TeamTown> town) {
        private static final ClaimResult DENIED = new ClaimResult(false, null, Optional.empty());
    }
}

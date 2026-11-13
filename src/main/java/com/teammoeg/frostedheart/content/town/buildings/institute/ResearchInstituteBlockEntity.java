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

package com.teammoeg.frostedheart.content.town.buildings.institute;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.bootstrap.common.FHCapabilities;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.content.steamenergy.HeatEndpoint;
import com.teammoeg.frostedheart.content.town.TownMathFunctions;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlockEntity;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.content.town.block.blockscanner.BlockScanner.RoomData;
import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import com.teammoeg.frostedheart.content.town.provider.TeamTownProvider;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.client.FHClientUtils;
import com.teammoeg.frostedresearch.api.TeamResearchService;
import com.teammoeg.frostedresearch.blocks.DrawingDeskBlock;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

/**
 * 研究所核心：房间扫描统计书架、绘图台与装饰，环境刷新采样温度与方块光，北面接热网。
 * <p>
 * Research institute core. The room scan counts bookshelves, drawing desks and decorations; the environment refresh
 * samples temperature and block light; the north face takes heat from the steam network like a hunting base.
 */
public class ResearchInstituteBlockEntity extends AbstractTownBuildingBlockEntity<ResearchInstituteBuilding> implements MenuProvider {
    private final HeatEndpoint endpoint;
    private final LazyOptional<HeatEndpoint> endpointCap;
    @Getter
    private double temperatureModifier = 0;
    /** Capacity last reported to the knowledge system; -1 before the first check after loading. */
    private int notifiedArchive = -1, notifiedInbox = -1;

    public ResearchInstituteBlockEntity(BlockPos pos, BlockState state) {
        super(FHBlockEntityTypes.RESEARCH_INSTITUTE.get(), pos, state);
        FHConfig.Server.Town.ResearchInstitute config = FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE;
        endpoint = HeatEndpoint.consumer(config.heatEndpointPriority.get(), config.heatConsumptionPerTick.get().floatValue());
        endpointCap = LazyOptional.of(() -> endpoint);
    }

    @Override
    public StructureScanResult scanStructure(ResearchInstituteBuilding building) {
        RoomData room = BlockScanner.scanRoomDataFromBlock(level, worldPosition);
        if (room == null) {
            return StructureScanResult.DATA_UNAVAILABLE;
        }
        building.setRoadEntrances(room.doors);
        if (room.doors.isEmpty()) {
            room = room.withValidity(false);
        }
        if (!room.isValid()) {
            building.setOccupiedVolume(room.occupiedCells);
            return StructureScanResult.INVALID;
        }
        // Bookshelves and desks have (nearly) full collision, so the scanner treats them as walls: count the blocks
        // inside the room and those bordering it, as the warehouse does for its racks.
        int bookshelves = room.countAllBlock(state -> state.is(FHTags.Blocks.INSTITUTE_BOOKSHELVES.get()));
        int desks = room.countAllBlock(ResearchInstituteBlockEntity::isDesk);
        FHConfig.Server.Town.Housing housing = FHConfig.SERVER.TOWN.HOUSING;
        double decorationRating = TownMathFunctions.calculateDecorationRating(
                room.findAllBlock(state -> state.is(FHTags.Blocks.INSTITUTE_DECORATIONS.get())),
                room.area, housing.decorationCountLogOffset.get(), housing.decorationCountLogMultiplier.get(),
                housing.decorationTypeBaseScore.get(), housing.decorationBaseDemand.get(),
                housing.decorationFloorBlocksPerDemand.get());
        FHConfig.Server.Town.BuildingScoring scoring = FHConfig.SERVER.TOWN.BUILDING_SCORING;
        double spaceRating = TownMathFunctions.calculateSpaceRating(room.volume, room.area,
                scoring.spaceAreaCoefficient.get(), scoring.spaceHeightLogCoefficient.get(), scoring.spaceHeightLogOffset.get(),
                scoring.spaceResponseScale.get(), scoring.spaceResponseExponent.get());
        int slots = ResearchInstituteDailyModel.researcherSlots(Double.isFinite(spaceRating) ? spaceRating : 0, room.area,
                FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE.floorBlocksPerResearcher.get(), desks);
        building.setOccupiedVolume(room.occupiedCells);
        building.setArea(room.area);
        building.setVolume(room.volume);
        building.setBookshelves(bookshelves);
        building.setDesks(desks);
        building.setDecorationRating(Double.isFinite(decorationRating) ? decorationRating : 0);
        building.setMaxResidents(slots);
        return StructureScanResult.VALID;
    }

    /** A research desk; the secondary half of a two-block drawing desk is not a second desk. */
    static boolean isDesk(BlockState state) {
        if (!state.is(FHTags.Blocks.RESEARCH_DESKS.get())) return false;
        return !state.hasProperty(DrawingDeskBlock.IS_NOT_MAIN) || !state.getValue(DrawingDeskBlock.IS_NOT_MAIN);
    }

    @Override
    protected void refreshEnvironment(ResearchInstituteBuilding building) {
        double temperature = BlockScanner.sampleTemperature(level, building.getOccupiedVolume());
        if (Double.isFinite(temperature)) {
            building.setTemperature(temperature);
        }
        double light = BlockScanner.sampleBlockLight(level, building.getOccupiedVolume());
        if (Double.isFinite(light)) {
            building.setAverageLight(light);
        }
        building.setTemperatureModifier(temperatureModifier);
        reportCapacity(building);
    }

    /**
     * The knowledge view shows capacity, so a change in what this institute grants marks its team for a knowledge sync.
     */
    private void reportCapacity(ResearchInstituteBuilding building) {
        int archive = building.archiveCapacity(), inbox = building.inboxCapacity();
        if (archive == notifiedArchive && inbox == notifiedInbox) return;
        notifiedArchive = archive;
        notifiedInbox = inbox;
        if (townProvider instanceof TeamTownProvider provider && provider.ownerUUID != null && CTeamDataManager.INSTANCE != null) {
            TeamDataHolder team = CTeamDataManager.INSTANCE.findByHolderId(provider.ownerUUID);
            if (team != null) TeamResearchService.sync(team);
        }
    }

    @Override
    public void tick() {
        double previousModifier = temperatureModifier;
        assert level != null;
        if (!level.isClientSide) {
            FHConfig.Server.Town.ResearchInstitute config = FHConfig.SERVER.TOWN.RESEARCH_INSTITUTE;
            double heatConsumptionPerTick = config.heatConsumptionPerTick.get();
            if (heatConsumptionPerTick > 0.0 && endpoint.tryDrainHeat((float) heatConsumptionPerTick)) {
                temperatureModifier = Math.max(endpoint.getTempLevel() * config.heatTemperatureLevelScaleCelsius.get(),
                        config.minimumHeatingModifierCelsius.get());
                if (setActive(true)) setChanged();
            } else {
                temperatureModifier = 0;
                if (setActive(false)) setChanged();
            }
        } else if (getIsActive()) {
            FHClientUtils.spawnSteamParticles(level, worldPosition);
        }
        if (!level.isClientSide && Double.compare(previousModifier, temperatureModifier) != 0) {
            getBuilding().ifPresent(building -> {
                building.updateBatch(() -> building.setTemperatureModifier(temperatureModifier));
                reportCapacity(building);
            });
        }
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean isPacket) {
        super.readCustomNBT(nbt, isPacket);
        endpoint.load(nbt, isPacket);
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean isPacket) {
        super.writeCustomNBT(nbt, isPacket);
        endpoint.save(nbt, isPacket);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, Direction facing) {
        if (capability == FHCapabilities.HEAT_EP.capability() && facing == Direction.NORTH) {
            return endpointCap.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        endpointCap.invalidate();
        super.invalidateCaps();
    }

    @Override
    public @Nullable ResearchInstituteBuilding getBuilding(AbstractTownBuilding building) {
        return building instanceof ResearchInstituteBuilding institute ? institute : null;
    }

    @Override
    public @NotNull ResearchInstituteBuilding createBuilding() {
        return new ResearchInstituteBuilding(getBlockPos());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, @NotNull Inventory inventory, @NotNull Player player) {
        return new ResearchInstituteMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.frostedheart.research_institute");
    }
}

/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.buildings.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.block.OccupiedVolume;
import com.teammoeg.frostedheart.content.town.building.AbstractTownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * 货运站的持久化建筑状态。
 * <p>
 * Persistent building state for a transport station. The initial foundation
 * stage records its validated structure and worker roster but does not
 * produce transport capacity yet.
 */
public class TransportStationBuilding extends AbstractTownResidentWorkBuilding {
    public static final Codec<TransportStationBuilding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.optionalFieldOf("pos", BlockPos.ZERO).forGetter(building -> building.pos),
            Codec.BOOL.optionalFieldOf("initialized", false).forGetter(TransportStationBuilding::isInitialized),
            Codec.BOOL.optionalFieldOf("occupiedAreaOverlapped", false)
                    .forGetter(TransportStationBuilding::isOccupiedAreaOverlapped),
            Codec.BOOL.optionalFieldOf("isStructureValid", false)
                    .forGetter(TransportStationBuilding::isStructureValid),
            OccupiedVolume.CODEC.optionalFieldOf("occupiedVolume", OccupiedVolume.EMPTY)
                    .forGetter(TransportStationBuilding::getOccupiedVolume),
            UUIDUtil.CODEC.listOf().optionalFieldOf("residentsID", List.of())
                    .forGetter(building -> building.getResidentsID().stream().sorted().toList()),
            Codec.INT.optionalFieldOf("area", 0).forGetter(TransportStationBuilding::getArea),
            Codec.INT.optionalFieldOf("volume", 0).forGetter(TransportStationBuilding::getVolume),
            Codec.INT.optionalFieldOf("maxResidents", 0).forGetter(TransportStationBuilding::getMaxResidents)
    ).apply(instance, TransportStationBuilding::new));

    @Getter
    private int area;
    @Getter
    private int volume;

    public TransportStationBuilding(BlockPos pos) {
        super(pos);
    }

    public TransportStationBuilding(
            BlockPos pos,
            boolean initialized,
            boolean occupiedAreaOverlapped,
            boolean structureValid,
            OccupiedVolume occupiedVolume,
            List<UUID> residentsID,
            int area,
            int volume,
            int maxResidents
    ) {
        super(pos);
        setInitialized(initialized);
        setOccupiedAreaOverlapped(occupiedAreaOverlapped);
        setIsStructureValid(structureValid);
        setOccupiedVolume(occupiedVolume);
        this.residentsID = new HashSet<>(residentsID);
        setArea(area);
        setVolume(volume);
        setMaxResidents(maxResidents);
    }

    public void setArea(int area) {
        if (this.area == area) return;
        this.area = area;
        fireChange();
    }

    public void setVolume(int volume) {
        if (this.volume == volume) return;
        this.volume = volume;
        fireChange();
    }

    @Override
    public boolean isBuildingWorkable() {
        return super.isBuildingWorkable() && isSpaceValid();
    }

    public boolean isSpaceValid() {
        FHConfig.Server.Town.TransportStation config = FHConfig.SERVER.TOWN.TRANSPORT_STATION;
        return area >= config.minimumFloorAreaBlocks.get()
                && volume >= config.minimumInteriorVolumeBlocks.get();
    }

    @Override
    public boolean work(ITownWithBuildings town, ServerLevel world) {
        return false;
    }

    @Override
    public double getResidentScore(Resident resident) {
        return 0.0;
    }
}

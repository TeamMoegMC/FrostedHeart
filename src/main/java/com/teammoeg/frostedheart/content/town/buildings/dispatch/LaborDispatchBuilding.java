package com.teammoeg.frostedheart.content.town.buildings.dispatch;

import com.teammoeg.frostedheart.content.town.building.AbstractTownBuilding;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import static com.teammoeg.frostedheart.content.town.building.BuildingRecordsCodec.strict;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.block.OccupiedZoneInfo;
import com.teammoeg.frostedheart.content.town.building.AbstractTownResidentWorkBuilding;
import com.teammoeg.frostedheart.content.town.resident.Resident;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;

public class LaborDispatchBuilding extends AbstractTownResidentWorkBuilding {
    public static final MapCodec<LaborDispatchBuilding> MAP_CODEC = AbstractTownBuilding.named(RecordCodecBuilder.mapCodec(instance -> instance.group(
            strict(BlockPos.CODEC).optionalFieldOf("pos", BlockPos.ZERO).forGetter(building -> building.pos),
            strict(Codec.BOOL).optionalFieldOf("initialized", false).forGetter(LaborDispatchBuilding::isInitialized),
            strict(Codec.BOOL).optionalFieldOf("isStructureValid", false)
                    .forGetter(LaborDispatchBuilding::isStructureValid),
            strict(OccupiedZoneInfo.CODEC).optionalFieldOf("occupiedVolume",OccupiedZoneInfo.EMPTY)
                    .forGetter(t->t.getOccupiedVolume()),
            strict(UUIDUtil.CODEC.listOf()).optionalFieldOf("residentsID", List.of())
                    .forGetter(building -> building.getResidentsID().stream().sorted().toList()),
            strict(Codec.INT).optionalFieldOf("area", 0).forGetter(LaborDispatchBuilding::getArea),
            strict(Codec.INT).optionalFieldOf("volume", 0).forGetter(LaborDispatchBuilding::getVolume),
            strict(Codec.INT).optionalFieldOf("maxResidents", 0).forGetter(LaborDispatchBuilding::getMaxResidents)

    ).apply(instance, LaborDispatchBuilding::new)));
        public static final Codec<LaborDispatchBuilding> CODEC = MAP_CODEC.codec();


    @Getter
    private int area;
    @Getter
    private int volume;

    public LaborDispatchBuilding(BlockPos pos) {
        super(pos);
    }

    public LaborDispatchBuilding(BlockPos pos, boolean initialized, boolean valid, OccupiedZoneInfo occupied,
            List<UUID> residents, int area, int volume, int slots) {
        super(pos);
        setInitialized(initialized);
        setIsStructureValid(valid);
        internalSetOccupiedVolume(occupied);
        residentsID = new HashSet<>(residents);
        setArea(area);
        setVolume(volume);
        setMaxResidents(slots);
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
        return area >= 9 && volume >= 18;
    }

    public int availableWorkers(TeamTown town) {
        if (!isBuildingWorkable()) return 0;
        return (int) getResidents(town).stream().filter(Objects::nonNull).filter(this::canResidentWork).count();
    }

    @Override
    public boolean work(ITownWithBuildings town, ServerLevel world) { return false; }

    @Override
    public double getResidentScore(Resident resident) { return resident == null ? 0 : 1; }
}

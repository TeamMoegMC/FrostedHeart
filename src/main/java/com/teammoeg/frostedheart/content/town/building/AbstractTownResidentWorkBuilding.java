package com.teammoeg.frostedheart.content.town.building;

import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.ITownWithResidents;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import lombok.Getter;
import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public abstract class AbstractTownResidentWorkBuilding extends AbstractTownBuilding implements ITownResidentWorkBuilding{
    protected Set<UUID> residentsID = new HashSet<>();
    @Getter
    public int maxResidents;

    protected AbstractTownResidentWorkBuilding(BlockPos pos) {
        super(pos);
    }

    @Override
    public void onRemoved(ITownWithBuildings buildingTown) {
        if(buildingTown instanceof ITownWithResidents residentTown){
            for(UUID uuid : this.residentsID){
                residentTown.getResident(uuid).ifPresent(resident -> {
                    resident.setWorkPos(null);
                });
            }
        }
    }

    public boolean addResident(Resident resident){
        resident.setWorkPos(this.getPos());
        return this.residentsID.add(resident.getUUID());
    }

    public boolean removeResident(Resident resident){
        resident.setWorkPos(null);
        return this.residentsID.remove(resident.getUUID());
    }

    @Override
    public Set<UUID> getResidentsID(){
        return residentsID;
    }

}

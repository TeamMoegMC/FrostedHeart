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
    private int maxResidents;

    public void setMaxResidents(int maxResidents) {
        this.maxResidents = maxResidents;
        fireChange();
    }

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
        boolean added = this.residentsID.add(resident.getUUID());
        if (added) fireChange();
        return added;
    }

    public boolean removeResident(Resident resident){
        resident.setWorkPos(null);
        boolean removed = this.residentsID.remove(resident.getUUID());
        if (removed) fireChange();
        return removed;
    }

    @Override
    public Set<UUID> getResidentsID(){
        return residentsID;
    }

}

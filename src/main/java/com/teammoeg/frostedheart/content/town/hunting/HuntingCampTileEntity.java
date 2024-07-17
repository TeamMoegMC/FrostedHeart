package com.teammoeg.frostedheart.content.town.hunting;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerTileEntity;
import com.teammoeg.frostedheart.content.town.TownWorkerState;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import com.teammoeg.frostedheart.util.blockscanner.BlockScanner;
import com.teammoeg.frostedheart.util.blockscanner.ConfinedSpaceScanner;
import net.minecraft.nbt.CompoundNBT;

public class HuntingCampTileEntity extends AbstractTownWorkerTileEntity {
    public HuntingCampTileEntity() {
        super(FHTileTypes.HUNTING_CAMP.get());
    }

    private boolean isStructureValid(){
        ConfinedSpaceScanner confinedSpaceScanner = new ConfinedSpaceScanner(this.world, pos.up());
        return !confinedSpaceScanner.scan(256);
    }

    
    @Override
    public void refresh() {
        this.occupiedArea.add(BlockScanner.toColumnPos(pos));
        if(this.workerState == TownWorkerState.OCCUPIED_AREA_OVERLAPPED) return;
        this.workerState = isStructureValid()?TownWorkerState.VALID:TownWorkerState.NOT_VALID;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public TownWorkerType getWorkerType() {
        return TownWorkerType.HUNTING_CAMP;
    }

    @Override
    public CompoundNBT getWorkData() {
        return getBasicWorkData();
    }

    @Override
    public void setWorkData(CompoundNBT data) {
        setBasicWorkData(data);
    }
}

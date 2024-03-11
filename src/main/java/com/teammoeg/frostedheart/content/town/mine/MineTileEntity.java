package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseTileEntity;
import com.teammoeg.frostedheart.base.block.FHBlockInterfaces;
import com.teammoeg.frostedheart.content.town.TownTileEntity;
import com.teammoeg.frostedheart.content.town.TownWorkerType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;

public class MineTileEntity extends FHBaseTileEntity implements TownTileEntity, ITickableTileEntity,
        FHBlockInterfaces.IActiveState{
    public int avgLightLevel;
    public int validStoneOrOre;

    public MineTileEntity(){
        super(FHTileTypes.MINE.get());
    }

    public boolean isStructureValid(){
        MineBlockScanner scanner = new MineBlockScanner(world, this.getPos().up());
        if(scanner.scan()){
            this.avgLightLevel = scanner.light;
            this.validStoneOrOre = scanner.validStone;
            return true;
        }
        return false;
    }

    @Override
    public void readCustomNBT(CompoundNBT compoundNBT, boolean b) {

    }

    @Override
    public void writeCustomNBT(CompoundNBT compoundNBT, boolean b) {

    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public TownWorkerType getWorker() {
        return TownWorkerType.MINE;
    }

    @Override
    public boolean isWorkValid() {
        return this.isStructureValid();
    }

    @Override
    public CompoundNBT getWorkData() {
        return null;
    }

    @Override
    public void setWorkData(CompoundNBT data) {

    }

    @Override
    public void tick() {

    }
}

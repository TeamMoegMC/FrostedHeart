package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.TownResourceType;
import com.teammoeg.frostedheart.content.town.TownWorker;
import net.minecraft.nbt.CompoundTag;

public class WarehouseWorker implements TownWorker {
    @Override
    public boolean firstWork(Town town, CompoundTag workData) {
        double capacity = workData.getCompound("tileEntity").getDouble("capacity");
        return capacity == town.add(TownResourceType.STORAGE, capacity, false);

    }

    @Override
    public boolean work(Town town, CompoundTag workData) {
        return false;
    }
}

package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.resource.ItemResourceType;
import com.teammoeg.frostedheart.content.town.TownWorker;
import com.teammoeg.frostedheart.content.town.resource.VirtualResourceType;
import net.minecraft.nbt.CompoundTag;

public class WarehouseWorker implements TownWorker {
    @Override
    public boolean firstWork(Town town, CompoundTag workData) {
        double capacity = workData.getCompound("tileEntity").getDouble("capacity");
        town.getResourceManager().addIfHaveCapacity(VirtualResourceType.MAX_CAPACITY.generateKey(0), capacity);
        return true;

    }

    @Override
    public boolean work(Town town, CompoundTag workData) {
        return false;
    }
}

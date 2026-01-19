package com.teammoeg.frostedheart.content.town;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.Optional;

import com.teammoeg.frostedheart.content.town.worker.TownWorkerData;

public interface ITownWithBlocks {
    Map<BlockPos, TownWorkerData> getTownBlocks();

    default Optional<TownWorkerData> getTownBlock(BlockPos pos){
        return Optional.ofNullable(getTownBlocks().get(pos));
    }

    void addTownBlock(BlockPos pos, TownBlockEntity tile);

    void removeTownBlock(BlockPos pos);
}
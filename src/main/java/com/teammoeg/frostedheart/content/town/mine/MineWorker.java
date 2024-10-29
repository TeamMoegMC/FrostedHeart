package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.frostedheart.content.town.*;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedheart.util.MathUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.*;
import java.util.stream.Collectors;

public class MineWorker implements TownWorker {
    @Override
    public boolean work(Town town, CompoundTag workData) {
        if(town instanceof TownWithResident){
            TeamTown teamTown = (TeamTown) town;
            CompoundTag dataTE = workData.getCompound("tileEntity");
            double rating = dataTE.getDouble("rating");
            ListTag list = dataTE.getList("resources", Tag.TAG_COMPOUND);
            EnumMap<TownResourceType, Double> resources = new EnumMap<>(TownResourceType.class);
            list.forEach(nbt -> {
                CompoundTag nbt_1 = (CompoundTag) nbt;
                String key = nbt_1.getString("type");
                double amount = nbt_1.getDouble("amount");
                resources.put(TownResourceType.from(key), amount);
            });
            List<Resident> residents = workData.getCompound("town").getList("residents", Tag.TAG_STRING)
                    .stream()
                    .map(nbt -> UUID.fromString(nbt.getAsString()))
                    .map(teamTown::getResident)
                    .map(optional -> optional.orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            for(Resident resident : residents){
                double add = rating * resident.getWorkScore(TownWorkerType.MINE);
                double randomDouble = MathUtils.RANDOM.nextDouble();
                double counter = 0;
                for(Map.Entry<TownResourceType, Double> entry : resources.entrySet()){
                    counter += entry.getValue();
                    if(counter >= randomDouble){
                        double actualAdd = town.add(entry.getKey(), add, false);
                        if(add != actualAdd) return false;
                    }
                }
            }
            return true;
        }
        return false;
    }
}
package com.teammoeg.frostedheart.content.town.building;

import com.teammoeg.frostedheart.content.town.ITownWithResidents;
import com.teammoeg.frostedheart.content.town.resident.Resident;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 所有需要居民参与工作的城镇建筑。
 * 在TeamTownData中，居民分配工作时，会调用此接口中的方法。
 */
public interface ITownResidentWorkBuilding extends ITownResidentBuilding {
    //boolean addResident(UUID residentID);

    @Override
    default Collection<Resident> getResidents(ITownWithResidents townOfBuilding){
        Collection<UUID> residentsID = getResidentsID();
        Collection<Resident> residents = townOfBuilding.getAllResidents();
        return residents.stream()
                .filter(resident -> residentsID.contains(resident.getUUID()))
                .collect(Collectors.toSet());
    }

    /**
     * 获取城镇在分配工作时，该建筑挑选居民的优先级。
     * 优先级越高，居民便会优先进入。
     * 优先级通常会随着工作分配的过程动态变化。
     * <br>
     * 每有一个居民在此工作，优先级应减少1左右，这样可以使居民尽可能均匀地分配在所有工作方块中。
     * <br>
     * 当居民数量大于最大居民数时，应该返回Double. NEGATIVE_INFINITY
     * <br>
     * 这个方法不应直接调用下面的那个同名方法，以避免重复读取nbt中的数据。
     */
    double getResidentPriority();

    /**
     * 获取居民在此种类工作方块工作的适合程度。
     * 决定居民的工作效率。
     * 建筑挑选居民时，分数高的居民会优先进入。
     */
    double getResidentScore(Resident resident);

    /**
     * 判断居民能否在此工作方块工作。
     * <br>
     * 可在子类覆写此方法，以对不同的工作设置工作条件。
     */
    default boolean canResidentWork(Resident resident){
        if(resident.getHealth() <= 10) return false;
        if(resident.getMental() <= 5) return false;
        if(resident.getHousePos() == null) return false;
        return true;
    }

    /**
     * 判断居民能否被分配到此工作方块。
     * 相比较 canResidentWork，此方法的区别在于会判断居民是否已有工作。
     * <br>
     * 此方法无需在子类覆写。
     */
    default boolean canResidentBeAssigned(Resident resident){
        if(resident.getWorkPos() == null) return true;
        return canResidentWork(resident);
    }
}

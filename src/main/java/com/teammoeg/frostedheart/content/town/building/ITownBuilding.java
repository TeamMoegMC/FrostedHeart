package com.teammoeg.frostedheart.content.town.building;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.frostedheart.content.town.Town;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingCampBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;

/**
 * it used to be TownWorker.
 * handle the work logic of town building.
 */
public interface ITownBuilding {
    public static final Codec<ITownBuilding> CODEC = CodecUtil.dispatch(ITownBuilding.class)
            .type("house", HouseBuilding.class, HouseBuilding.CODEC)
            .type("huntingCamp", HuntingCampBuilding.class, HuntingCampBuilding.CODEC)
            .type("huntingBase", HuntingBaseBuilding.class, HuntingBaseBuilding.CODEC)
            .type("mine", MineBuilding.class, MineBuilding.CODEC)
            .type("mineBase", MineBaseBuilding.class, MineBaseBuilding.CODEC)
            .type("warehouse", WarehouseBuilding.class, WarehouseBuilding.CODEC)
            .buildByInt();

    /**
     * check if this building is workable.
     * @return true if workable
     */
    boolean isBuildingWorkable();

    /**
     * Work logic of this building
     * @param town town of this building
     * @return true if worked successful
     */
    boolean work(Town town);

    /**
     * 获取城镇工作时该建筑参与工作的优先级。
     * 这个值应该是只和类型相关的常量。
     * <br>
     * 优先级越高，则越先工作。
     * 默认优先级为0。
     * <br>
     * 一般来说，此优先级不会影响工作本身。
     * @return 工作优先级。
     */
    default int getWorkPriority(){
        return DEFAULT_PRIORITY;
    }
    int DEFAULT_PRIORITY=0;

    void onRemoved(Town town);
}

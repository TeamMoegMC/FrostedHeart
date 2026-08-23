package com.teammoeg.frostedheart.content.town.building;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.buildings.logistics.TransportStationBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import net.minecraft.server.level.ServerLevel;

/**
 * it used to be TownWorker.
 * handle the work logic of town building.
 */
public interface ITownBuilding {
    public static final Codec<ITownBuilding> CODEC = CodecUtil.dispatch(ITownBuilding.class)
            .typeLazy("house", HouseBuilding.class, () -> HouseBuilding.CODEC)
            .typeLazy("huntingBase", HuntingBaseBuilding.class, () -> HuntingBaseBuilding.CODEC)
            .typeLazy("mine", MineBuilding.class, () -> MineBuilding.CODEC)
            .typeLazy("mineBase", MineBaseBuilding.class, () -> MineBaseBuilding.CODEC)
            .typeLazy("warehouse", WarehouseBuilding.class, () -> WarehouseBuilding.CODEC)
            .typeLazy("transportStation", TransportStationBuilding.class, () -> TransportStationBuilding.CODEC)
            // 按字符串key分发（写盘存 "house"/"mine" 等名称），解码时兼容旧存档的整数索引。
            // 新增建筑类型时可任意位置插入，不再受注册顺序约束。
            .buildByNameWithLegacyInt();

    /**
     * check if this building is workable.
     * @return true if workable
     */
    boolean isBuildingWorkable();

    /**
     * Whether this building should participate in the town's daily settlement.
     * <p>
     * Most buildings only settle while they are workable. Buildings whose
     * daily logic represents an obligation rather than optional production may
     * override this independently from {@link #isBuildingWorkable()}.
     */
    default boolean shouldRunDailySettlement() {
        return isBuildingWorkable();
    }

    /**
     * Work logic of this building
     * @param town town of this building
     * @return true if worked successful
     */
    boolean work(ITownWithBuildings town, ServerLevel world);

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

    void onRemoved(ITownWithBuildings town);
}

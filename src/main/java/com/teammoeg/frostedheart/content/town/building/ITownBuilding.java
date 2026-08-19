package com.teammoeg.frostedheart.content.town.building;

import com.mojang.serialization.Codec;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.teammoeg.chorda.io.CodecUtil;
import com.teammoeg.frostedheart.content.town.ITownWithBuildings;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.buildings.logistics.TransportStationBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import net.minecraft.server.level.ServerLevel;

import java.util.function.Supplier;

/**
 * it used to be TownWorker.
 * handle the work logic of town building.
 */
public interface ITownBuilding {
    public static final Codec<ITownBuilding> CODEC = CodecUtil.dispatch(ITownBuilding.class)
            .type("house", HouseBuilding.class, HouseBuilding.CODEC)
            .type("huntingBase", HuntingBaseBuilding.class, HuntingBaseBuilding.CODEC)
            .type("mine", MineBuilding.class, MineBuilding.CODEC)
            .type("mineBase", MineBaseBuilding.class, MineBaseBuilding.CODEC)
            .type("warehouse", WarehouseBuilding.class, WarehouseBuilding.CODEC)
            .type("transportStation", TransportStationBuilding.class,
                    lazyCodec(() -> TransportStationBuilding.CODEC))
            // 按字符串key分发（写盘存 "house"/"mine" 等名称），解码时兼容旧存档的整数索引。
            // 新增建筑类型时可任意位置插入，不再受注册顺序约束。
            .buildByNameWithLegacyInt();

    /**
     * Defers resolving a concrete codec until after its class has completed
     * initialization. This avoids the abstract building codec's static
     * dispatch table observing a null concrete codec during class loading.
     */
    private static <T> Codec<T> lazyCodec(Supplier<Codec<T>> codecSupplier) {
        return Codec.of(new Encoder<>() {
            @Override
            public <O> DataResult<O> encode(T input, DynamicOps<O> ops, O prefix) {
                return codecSupplier.get().encode(input, ops, prefix);
            }
        }, new Decoder<>() {
            @Override
            public <O> DataResult<Pair<T, O>> decode(DynamicOps<O> ops, O input) {
                return codecSupplier.get().decode(ops, input);
            }
        });
    }

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

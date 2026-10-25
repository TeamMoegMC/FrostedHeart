package com.teammoeg.frostedheart.content.town.building;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.teammoeg.frostedheart.content.town.buildings.house.HouseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.hunting.HuntingBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.logistics.TransportStationBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBaseBuilding;
import com.teammoeg.frostedheart.content.town.buildings.mine.MineBuilding;
import com.teammoeg.frostedheart.content.town.buildings.warehouse.WarehouseBuilding;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Stable storage identities. Legacy numbers are deliberately independent of registration order. */
public final class TownBuildingTypes {
    public static final int VERSION = 1;
    private static long resourceGeneration;
    private static final List<Type<?>> TYPES = List.of(
            new Type<>("house", HouseBuilding.class, () -> HouseBuilding.MAP_CODEC, () -> HouseBuilding.DailyReport.CODEC),
            new Type<>("huntingBase", HuntingBaseBuilding.class, () -> HuntingBaseBuilding.MAP_CODEC, () -> HuntingBaseBuilding.HuntingDailyReport.CODEC),
            new Type<>("mine", MineBuilding.class, () -> MineBuilding.MAP_CODEC, null),
            new Type<>("mineBase", MineBaseBuilding.class, () -> MineBaseBuilding.MAP_CODEC, () -> MineBaseBuilding.MiningDailyReport.CODEC),
            new Type<>("warehouse", WarehouseBuilding.class, () -> WarehouseBuilding.MAP_CODEC, null),
            new Type<>("transportStation", TransportStationBuilding.class, () -> TransportStationBuilding.MAP_CODEC, () -> TransportStationBuilding.TransportStationDailyReport.CODEC));
    private static final List<String> LEGACY = List.of("house", "huntingBase", "mine", "mineBase", "warehouse", "transportStation");

    private TownBuildingTypes() {}

    public static Type<?> byId(Tag id) {
        if (id == null) {
            return null;
        }
        String name = id.getId() == Tag.TAG_STRING ? id.getAsString() : null;
        if (id instanceof net.minecraft.nbt.NumericTag number) {
            int index = number.getAsInt();
            name = number.getAsDouble() == index && index >= 0 && index < LEGACY.size() ? LEGACY.get(index) : null;
        }
        for (Type<?> type : TYPES) {
            if (type.id.equals(name)) {
                return type;
            }
        }
        return null;
    }

    public static Type<?> of(AbstractTownBuilding building) {
        for (Type<?> type : TYPES) {
            if (type.javaType == building.getClass()) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unregistered town building " + building.getClass().getName());
    }

    public static long resourceGeneration() {
        return resourceGeneration;
    }
    public static void resourcesReloaded() {
        resourceGeneration++;
    }

    public static final class Type<T extends AbstractTownBuilding> {
        private final String id;
        private final Class<T> javaType;
        private final Supplier<MapCodec<T>> codec;
        private final Supplier<? extends Codec<?>> report;
        private Set<String> ownedKeys;

        private Type(String id, Class<T> javaType, Supplier<MapCodec<T>> codec, Supplier<? extends Codec<?>> report) {
            this.id = id;
            this.javaType = javaType;
            this.codec = codec;
            this.report = report;
        }

        public String id() {
            return id;
        }
        public Codec<T> codec() {
            return codec.get().codec();
        }
        public Codec<?> reportCodec() {
            return report == null ? null : report.get();
        }
        public Set<String> ownedKeys() {
            if (ownedKeys == null) {
                ownedKeys = codec.get().keys(NbtOps.INSTANCE)
                        .map(Tag::getAsString).collect(Collectors.toUnmodifiableSet());
            }
            return ownedKeys;
        }
        public Tag encode(AbstractTownBuilding building) {
            return codec().encodeStart(NbtOps.INSTANCE, javaType.cast(building)).getOrThrow(false,
                    message -> { throw new IllegalStateException(message); });
        }
    }
}

package com.teammoeg.frostedheart.content.town.building;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.teammoeg.frostedheart.content.town.block.OccupiedZoneInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/** Building-only recovery. A bad entry never changes the meaning of the entire collection. */
public final class BuildingRecordsCodec extends MapCodec<TownBuildingStore> {
    private enum Purpose {
        PERSISTENCE,
        MANAGEMENT
    }

    public static final BuildingRecordsCodec DISK = new BuildingRecordsCodec(Purpose.PERSISTENCE);
    public static final BuildingRecordsCodec MANAGEMENT = new BuildingRecordsCodec(Purpose.MANAGEMENT);
    private static final List<String> KEYS = List.of("blocks", "retainedBuildingData", "buildingDimension", "buildingLoadIssues");
    private static final Map<String, Codec<?>> SNAPSHOTS = Map.ofEntries(
            Map.entry("initialized", Codec.BOOL), Map.entry("isStructureValid", Codec.BOOL),
            Map.entry("area", Codec.INT),
            Map.entry("volume", Codec.INT), Map.entry("maxResident", Codec.INT), Map.entry("maxResidents", Codec.INT),
            Map.entry("temperature", Codec.DOUBLE), Map.entry("temperatureModifier", Codec.DOUBLE),
            Map.entry("decorationRating", Codec.DOUBLE), Map.entry("decorationAmount", Codec.INT),
            Map.entry("tanningRackNum", Codec.INT), Map.entry("rating", Codec.DOUBLE), Map.entry("capacity", Codec.DOUBLE),
            Map.entry("bedPositions", Codec.LONG_STREAM), Map.entry("entrancePosition", Codec.LONG),
            Map.entry("biomePath", ResourceLocation.CODEC));
    private final Purpose purpose;

    private BuildingRecordsCodec(Purpose purpose) {
        this.purpose = purpose;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return KEYS.stream().map(ops::createString);
    }

    @Override
    public <T> DataResult<TownBuildingStore> decode(DynamicOps<T> ops, MapLike<T> input) {
        TownBuildingStore store = new TownBuildingStore();
        readDimension(store, read(ops, input.get("buildingDimension")));
        Tag blocks = read(ops, input.get("blocks"));
        if (blocks instanceof ListTag list) {
            for (Tag raw : list) {
                readEntry(store, raw);
            }
        } else if (blocks != null) {
            archive(store, "container", null, "blocks", blocks, "BAD_CONTAINER");
        }
        if (purpose == Purpose.MANAGEMENT) {
            readIssues(store, read(ops, input.get("buildingLoadIssues")));
        } else {
            readRetained(store, read(ops, input.get("retainedBuildingData")));
            store.retryPending();
        }
        return DataResult.success(store);
    }

    private static void readDimension(TownBuildingStore store, Tag raw) {
        if (raw == null) {
            return;
        }
        ResourceLocation id = raw instanceof StringTag ? ResourceLocation.tryParse(raw.getAsString()) : null;
        if (id == null) {
            archive(store, "field", null, "buildingDimension", raw, "BAD_DIMENSION");
        } else {
            store.loadDimension(ResourceKey.create(Registries.DIMENSION, id));
        }
    }

    private static void readIssues(TownBuildingStore store, Tag rawIssues) {
        if (!(rawIssues instanceof ListTag list)) {
            return;
        }
        for (Tag raw : list) {
            if (raw instanceof CompoundTag tag) {
                store.addViewIssue(new TownBuildingStore.Issue(
                        decodePosition(tag.get("pos")), tag.getString("type"), tag.getString("reason")));
            }
        }
    }

    private static void readRetained(TownBuildingStore store, Tag retained) {
        if (retained == null) {
            return;
        }
        if (!(retained instanceof ListTag list)) {
            archive(store, "field", null, "retainedBuildingData", retained, "BAD_RETAINED_DATA");
            return;
        }
        for (Tag raw : list) {
            if (!(raw instanceof CompoundTag tag) || !tag.contains("raw")) {
                archive(store, "entry", null, "", raw, "BAD_RETAINED_ENTRY");
                continue;
            }
            CompoundTag copy = tag.copy();
            BlockPos pos = decodePosition(copy.get("pos"));
            boolean canRetry = "pending".equals(copy.getString("mode"))
                    && pos != null && !store.resolvedMap().containsKey(pos) && !store.isPending(pos);
            if (!canRetry) {
                copy.putString("mode", "archive");
            }
            store.retain(copy);
            if (!copy.equals(tag)) {
                store.markMigrated();
            }
        }
    }

    @Override
    public <T> RecordBuilder<T> encode(TownBuildingStore store, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        CompoundTag encoded = purpose == Purpose.MANAGEMENT ? store.captureManagementFields() : store.capturePersistenceFields();
        for (String key : encoded.getAllKeys()) {
            prefix.add(key, convert(ops, encoded.get(key)));
        }
        return prefix;
    }

    private static <T> Tag read(DynamicOps<T> ops, T value) {
        return value == null ? null : ops.convertTo(NbtOps.INSTANCE, value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T convert(DynamicOps<T> ops, Tag tag) {
        return (Object) ops == NbtOps.INSTANCE ? (T) tag : NbtOps.INSTANCE.convertTo(ops, tag);
    }

    public static BlockPos decodePosition(Tag raw) {
        if (raw == null) {
            return null;
        }
        return BlockPos.CODEC.parse(NbtOps.INSTANCE, raw).result().map(BlockPos::immutable).orElse(null);
    }
    public static Tag encodePosition(BlockPos pos) {
        return BlockPos.CODEC.encodeStart(NbtOps.INSTANCE, pos).getOrThrow(false, message -> {});
    }

    static void readEntry(TownBuildingStore store, Tag raw) {
        if (!(raw instanceof CompoundTag record)) {
            archive(store, "entry", null, "", raw, "BAD_ENTRY");
            return;
        }
        CompoundTag original = record.get("building") instanceof CompoundTag payload ? payload : null;
        BlockPos outerPos = decodePosition(record.get("pos"));
        BlockPos innerPos = original == null ? null : decodePosition(original.get("pos"));
        BlockPos pos = outerPos != null ? outerPos : innerPos;
        if (pos == null) {
            archive(store, "entry", null, "", record, "MISSING_POSITION");
            return;
        }
        if (store.resolvedMap().containsKey(pos)) {
            archive(store, "entry", pos, "", record, "DUPLICATE_POSITION");
            return;
        }
        if (original == null) {
            pending(store, pos, record, "BAD_BUILDING_PAYLOAD");
            return;
        }
        TownBuildingTypes.Type<?> type = TownBuildingTypes.byId(original.get("type"));
        String failure = type == null ? "UNKNOWN_TYPE" : null;
        int version = record.contains("version") ? record.getInt("version") : 0;
        if (record.contains("version") && (!(record.get("version") instanceof NumericTag number)
                || number.getAsDouble() != version)) {
            failure = "BAD_VERSION";
        } else if (version < 0 || version > TownBuildingTypes.VERSION) {
            failure = "FUTURE_VERSION";
        }
        if (failure != null) {
            pending(store, pos, record, failure);
            return;
        }

        CompoundTag body = original.copy();
        body.put("pos", encodePosition(pos));
        body.remove("type");
        List<CompoundTag> fragments = new ArrayList<>();
        if (record.contains("pos") && outerPos == null) {
            fragments.add(retained("archive", "field", pos, "pos", record.get("pos"), "POSITION_REPAIRED"));
        }
        if (original.contains("pos") && !pos.equals(innerPos)) {
            fragments.add(retained("archive", "field", pos, "building.pos", original.get("pos"), "POSITION_REPAIRED"));
        }
        if (body.contains("dailyReport") && type.reportCodec() != null && !parses(type.reportCodec(), body.get("dailyReport"))) {
            fragments.add(retained("archive", "field", pos, "dailyReport", body.get("dailyReport"), "REPORT_REPAIRED"));
            body.remove("dailyReport");
        }
        boolean rebuild = false;
        for (var field : SNAPSHOTS.entrySet()) {
            if (type.ownedKeys().contains(field.getKey())) {
                rebuild |= repairSnapshot(body, pos, field.getKey(), field.getValue(), fragments);
            }
        }
        // Resolve this codec at read time to avoid the base/subclass initialization cycle.
        rebuild |= repairSnapshot(body, pos, "occupiedVolume", OccupiedZoneInfo.CODEC, fragments);
        if (rebuild) {
            body.putBoolean("initialized", false);
            body.putBoolean("isStructureValid", false);
        }
        if (body.contains("lootRollCarry") && (!(body.get("lootRollCarry") instanceof NumericTag carry)
                || !Double.isFinite(carry.getAsDouble()) || carry.getAsDouble() < 0.0 || carry.getAsDouble() >= 1.0)) {
            pending(store, pos, record, "INVALID_BUSINESS_DATA");
            return;
        }
        try {
            AbstractTownBuilding building = type.codec().parse(NbtOps.INSTANCE, body).getOrThrow(false, message -> {});
            if (store.isPending(pos)) {
                // The first usable record wins; previous undecodable candidates become archival only.
                store.archivePendingAt(pos);
            }
            CompoundTag outer = record.copy();
            outer.remove("pos");
            outer.remove("version");
            outer.remove("building");
            CompoundTag extras = original.copy();
            type.ownedKeys().forEach(extras::remove);
            extras.remove("type");
            store.installDecoded(pos, building, outer, extras);
            fragments.forEach(store::retain);
            if (version != TownBuildingTypes.VERSION || outerPos == null || !fragments.isEmpty()
                    || original.get("type") instanceof NumericTag) {
                store.markMigrated();
            }
        } catch (RuntimeException ex) {
            pending(store, pos, record, "INVALID_BUSINESS_DATA");
        }
    }

    private static boolean repairSnapshot(CompoundTag body, BlockPos pos, String name,
            Codec<?> codec, List<CompoundTag> fragments) {
        Tag value = body.get(name);
        if (value == null) {
            return false;
        }
        boolean finite = !(value instanceof NumericTag number) || Double.isFinite(number.getAsDouble());
        if (finite && parses(codec, value)) {
            return false;
        }
        fragments.add(retained("archive", "field", pos, name, value, "SNAPSHOT_REPAIRED"));
        body.remove(name);
        return true;
    }

    private static boolean parses(Codec<?> codec, Tag value) {
        try {
            return codec.parse(NbtOps.INSTANCE, value).result().isPresent();
        } catch (RuntimeException ex) {
            return false;
        }
    }
    private static void pending(TownBuildingStore store, BlockPos pos, Tag raw, String reason) {
        String mode = store.isPending(pos) ? "archive" : "pending";
        store.retain(retained(mode, "entry", pos, "", raw, reason));
        store.markMigrated();
    }
    private static void archive(TownBuildingStore store, String kind, BlockPos pos, String name, Tag raw, String reason) {
        store.retain(retained("archive", kind, pos, name, raw, reason));
        store.markMigrated();
    }
    private static CompoundTag retained(String mode, String kind, BlockPos pos, String name, Tag raw, String reason) {
        CompoundTag tag = new CompoundTag();
        tag.putString("mode", mode);
        tag.putString("kind", kind);
        tag.putString("reason", reason);
        if (pos != null) {
            tag.put("pos", encodePosition(pos));
        }
        if (!name.isEmpty()) {
            tag.putString("name", name);
        }
        if (raw != null) {
            tag.put("raw", raw.copy());
        }
        return tag;
    }

    /** Missing is allowed; malformed present values remain a decode error. */
    public static <A> Codec<A> strict(Codec<A> original) {
        return new Codec<>() {
            @Override
            public <T> DataResult<T> encode(A value, DynamicOps<T> ops, T prefix) {
                return original.encode(value, ops, prefix);
            }
            @Override
            public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
                return original.decode(ops, input);
            }
            @Override
            public MapCodec<A> optionalFieldOf(String name, A fallback) {
                return optional(original, name, original.optionalFieldOf(name, fallback), fallback);
            }
            @Override
            public MapCodec<Optional<A>> optionalFieldOf(String name) {
                MapCodec<Optional<A>> delegate = original.optionalFieldOf(name);
                return new MapCodec<>() {
                    @Override
                    public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> map) {
                        T value = map.get(name);
                        return value == null ? DataResult.success(Optional.empty()) : original.parse(ops, value).map(Optional::of);
                    }
                    @Override
                    public <T> RecordBuilder<T> encode(Optional<A> value, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                        return delegate.encode(value, ops, prefix);
                    }
                    @Override
                    public <T> Stream<T> keys(DynamicOps<T> ops) {
                        return delegate.keys(ops);
                    }
                };
            }
        };
    }
    private static <A> MapCodec<A> optional(Codec<A> original, String name, MapCodec<A> delegate, A fallback) {
        return new MapCodec<>() {
            @Override
            public <T> DataResult<A> decode(DynamicOps<T> ops, MapLike<T> map) {
                T value = map.get(name);
                return value == null ? DataResult.success(fallback) : original.parse(ops, value);
            }
            @Override
            public <T> RecordBuilder<T> encode(A value, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                return delegate.encode(value, ops, prefix);
            }
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return delegate.keys(ops);
            }
        };
    }
}

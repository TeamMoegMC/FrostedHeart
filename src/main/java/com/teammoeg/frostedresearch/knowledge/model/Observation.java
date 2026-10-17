/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 原始观察快照。所有权属于记录自身，物品 NBT 在输入及读取时复制。
 */
public record Observation(UUID recordId, Type type, ResourceLocation object,
                          Map<String, ObservationValue> values, Source source, Optional<CompoundTag> itemData) {
    public enum Type {BLOCK, ENTITY, ITEM}

    public static final Codec<UUID> UUID_CODEC = Codec.STRING.comapFlatMap(s -> {
        try {
            return DataResult.success(UUID.fromString(s));
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Invalid UUID: " + s);
        }
    }, UUID::toString);
    public static final Codec<Observation> CODEC = RecordCodecBuilder.<Observation>create(i -> i.group(
            UUID_CODEC.fieldOf("record_id").forGetter(Observation::recordId),
            KnowledgeKey.enumCodec(Type.class).fieldOf("type").forGetter(Observation::type),
            ResourceLocation.CODEC.fieldOf("object").forGetter(Observation::object),
            Codec.unboundedMap(Codec.STRING, ObservationValue.CODEC).optionalFieldOf("values", Map.of()).forGetter(Observation::values),
            Source.CODEC.fieldOf("source").forGetter(Observation::source),
            CompoundTag.CODEC.optionalFieldOf("item_data").forGetter(Observation::itemData)
    ).apply(i, Observation::new)).comapFlatMap(o -> {
        for (var e : o.values.entrySet()) {
            var expected = ObservationFields.type(e.getKey());
            if (expected.isEmpty() || expected.get() != e.getValue().type())
                return DataResult.error(() -> "Unknown or mistyped observation field: " + e.getKey());
            if (!ObservationFields.applies(e.getKey(), o.type) && e.getValue().state() != ObservationValue.State.NOT_APPLICABLE)
                return DataResult.error(() -> "Field is not applicable to " + o.type + ": " + e.getKey());
        }
        if (o.type != Type.ITEM && o.itemData.isPresent())
            return DataResult.error(() -> "Only item observations have item_data");
        return DataResult.success(o);
    }, java.util.function.Function.identity());

    public Observation {
        values = Map.copyOf(values);
        itemData = itemData.map(CompoundTag::copy);
    }

    @Override
    public Optional<CompoundTag> itemData() {
        return itemData.map(CompoundTag::copy);
    }

    public KnowledgeKey key() {
        return KnowledgeKey.observation(recordId);
    }

    public boolean canExportNote() {
        return type != Type.ITEM;
    }

    /**
     * Compare recorded item content, independently of record ID and original observer.
     */
    public boolean sameItemContent(Observation other) {
        return type == Type.ITEM && other.type == Type.ITEM && object.equals(other.object)
                && values.equals(other.values) && itemData.equals(other.itemData);
    }

    public ObservationValue value(String field) {
        var valueType = ObservationFields.type(field).orElse(ObservationValue.ValueType.STRING);
        if (!ObservationFields.applies(field, type)) return ObservationValue.notApplicable(valueType);
        return switch (field) {
            case "object" -> ObservationValue.known(object.toString());
            case "type" -> ObservationValue.known(type.name().toLowerCase(java.util.Locale.ROOT));
            case "record_id" -> ObservationValue.known(recordId.toString());
            case "source_kind" -> ObservationValue.known(source.kind());
            case "original_observer" ->
                    source.originalObserver().map(v -> ObservationValue.known(v.toString())).orElseGet(() -> ObservationValue.unknown(valueType));
            case "organization" ->
                    source.organization().map(v -> ObservationValue.known(v.toString())).orElseGet(() -> ObservationValue.unknown(valueType));
            case "source_dimension" ->
                    source.dimension().map(v -> ObservationValue.known(v.toString())).orElseGet(() -> ObservationValue.unknown(valueType));
            case "source_position.x" ->
                    source.position().map(v -> ObservationValue.known(v.getX())).orElseGet(() -> ObservationValue.unknown(valueType));
            case "source_position.y" ->
                    source.position().map(v -> ObservationValue.known(v.getY())).orElseGet(() -> ObservationValue.unknown(valueType));
            case "source_position.z" ->
                    source.position().map(v -> ObservationValue.known(v.getZ())).orElseGet(() -> ObservationValue.unknown(valueType));
            default -> values.getOrDefault(field, ObservationValue.unknown(valueType));
        };
    }

    public record Source(String kind, Optional<UUID> originalObserver, Optional<UUID> organization,
                         Optional<ResourceLocation> dimension, Optional<BlockPos> position) {
        public static final Codec<Source> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("kind").forGetter(Source::kind),
                UUID_CODEC.optionalFieldOf("original_observer").forGetter(Source::originalObserver),
                UUID_CODEC.optionalFieldOf("organization").forGetter(Source::organization),
                ResourceLocation.CODEC.optionalFieldOf("dimension").forGetter(Source::dimension),
                BlockPos.CODEC.optionalFieldOf("position").forGetter(Source::position)
        ).apply(i, Source::new));

        public Source {
            position = position.map(BlockPos::immutable);
        }

        public static Source command() {
            return new Source("command", Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        }
    }
}

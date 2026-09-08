package com.teammoeg.frostedresearch.knowledge.observation;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClockSource;
import com.teammoeg.frostedresearch.knowledge.model.Observation;
import com.teammoeg.frostedresearch.knowledge.model.ObservationValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;

/**
 * 完成时一次采样，后续查看不读取世界。 Samples once when observation completes.
 */
public final class ObservationSampling {
    private ObservationSampling() {
    }

    public static Observation block(ServerPlayer player, BlockPos position) {
        BlockState state = player.level().getBlockState(position);
        Map<String, ObservationValue> values = environment(player, Vec3.atLowerCornerOf(position));
        for (Property<?> property : state.getProperties())
            values.put("block_state." + property.getName(), ObservationValue.known(propertyName(state, property)));
        return new Observation(UUID.randomUUID(), Observation.Type.BLOCK,
                BuiltInRegistries.BLOCK.getKey(state.getBlock()), values, source(player), Optional.empty());
    }

    public static Observation entity(ServerPlayer player, Entity target) {
        Map<String, ObservationValue> values = environment(player, target.position());
        values.put("entity_instance", ObservationValue.known(target.getUUID().toString()));
        return new Observation(UUID.randomUUID(), Observation.Type.ENTITY,
                BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()), values, source(player), Optional.empty());
    }

    public static Observation.Source source(ServerPlayer player) {
        return new Observation.Source("player_observation", Optional.of(player.getUUID()),
                Optional.empty(), Optional.empty(), Optional.empty());
    }

    private static <T extends Comparable<T>> String propertyName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static Map<String, ObservationValue> environment(ServerPlayer player, Vec3 location) {
        var world = player.serverLevel();
        BlockPos position = BlockPos.containing(location);
        Map<String, ObservationValue> values = new LinkedHashMap<>();
        values.put("dimension", ObservationValue.known(world.dimension().location().toString()));
        values.put("x", ObservationValue.known(location.x));
        values.put("y", ObservationValue.known(location.y));
        values.put("z", ObservationValue.known(location.z));
        values.put("time", ObservationValue.known(world.getGameTime()));
        values.put("biome", world.getBiome(position).unwrapKey()
                .map(key -> ObservationValue.known(key.location().toString()))
                .orElseGet(() -> ObservationValue.unknown(ObservationValue.ValueType.STRING)));
        float temperature = WorldTemperature.block(world, position);
        values.put("temperature", Float.isFinite(temperature) ? ObservationValue.known(temperature)
                : ObservationValue.unknown(ObservationValue.ValueType.NUMBER));
        values.put("temperature_type", Float.isFinite(temperature) ? ObservationValue.known(temperatureCategory(temperature))
                : ObservationValue.unknown(ObservationValue.ValueType.STRING));
        // Calendar comes from the climate clock snapshot; dimensions without one remain explicitly unknown.
        WorldClimate climate = WorldClimate.get(world);
        if (climate != null) {
            CompoundTag time = new CompoundTag();
            time.putLong("secs", climate.getSec());
            WorldClockSource clock = new WorldClockSource();
            clock.deserialize(time);
            Calendar calendar = clock.getGameCalendar();
            values.put("year", ObservationValue.known(calendar.get(Calendar.YEAR)));
            values.put("month", ObservationValue.known(calendar.get(Calendar.MONTH) + 1));
            values.put("day", ObservationValue.known(calendar.get(Calendar.DAY_OF_MONTH)));
            values.put("hour", ObservationValue.known(calendar.get(Calendar.HOUR_OF_DAY)));
            values.put("time_period", ObservationValue.known(timePeriod(calendar.get(Calendar.HOUR_OF_DAY))));
            values.put("climate", ObservationValue.known(climate.getClimate(new ChunkPos(position)).name().toLowerCase(Locale.ROOT)));
        }
        return values;
    }

    /**
     * Celsius thresholds used only for the descriptive context classification.
     */
    public static String temperatureCategory(double celsius) {
        return celsius < -20 ? "extreme_cold" : celsius < 0 ? "cold" : celsius < 30 ? "comfortable"
                : celsius < 45 ? "hot" : "extreme_heat";
    }

    public static String timePeriod(int hour) {
        return hour < 4 ? "midnight" : hour < 6 ? "predawn" : hour < 9 ? "dawn"
                : hour < 12 ? "morning" : hour < 14 ? "noon" : hour < 18 ? "afternoon" : hour < 21 ? "dusk" : "midnight";
    }
}

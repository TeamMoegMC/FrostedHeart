package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.thermal.persistence.DormantThermalCooling;
import com.teammoeg.frostedheart.content.climate.thermal.profile.minecraft.MinecraftThermalProfiles;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

/**
 * Sparse, saved phase-work progress for thin-ice cracks, using the material phase
 * temperature path. Partial stage work is separate from the body's enthalpy;
 * actual melting remains owned by the thermal runtime.
 */
public final class ThinIcePhaseData extends SavedData {
    private static final String ID = "frostedheart_thin_ice";
    private static final int INTERVAL = 20;
    private final Long2ObjectOpenHashMap<Progress> progress = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet active = new LongOpenHashSet();

    private static final class Progress {
        double workJ;
        boolean invalid;
        Progress(double workJ) { this.workJ = workJ; }
    }

    public static ThinIcePhaseData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ThinIcePhaseData::load, ThinIcePhaseData::new, ID);
    }

    static ThinIcePhaseData load(CompoundTag tag) {
        var data = new ThinIcePhaseData();
        for (var item : tag.getList("progress", Tag.TAG_COMPOUND)) {
            var entry = (CompoundTag) item;
            data.progress.put(entry.getLong("position"), new Progress(entry.getDouble("work_j")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        var list = new ListTag();
        for (var entry : progress.long2ObjectEntrySet()) {
            if (entry.getValue().invalid) continue;
            var item = new CompoundTag();
            item.putLong("position", entry.getLongKey());
            item.putDouble("work_j", entry.getValue().workJ);
            list.add(item);
        }
        tag.put("progress", list);
        return tag;
    }

    public static void discard(ServerLevel level, BlockPos pos) {
        var data = level.getDataStorage().get(ThinIcePhaseData::load, ID);
        if (data == null) return;
        var entry = data.progress.get(pos.asLong());
        if (entry != null) {
            entry.invalid = true;
            data.progress.remove(pos.asLong());
            data.setDirty();
        }
    }

    static double temperature(ServerLevel level, BlockPos pos) {
        return MinecraftPhaseController.temperature(level, pos);
    }

    public static int warmLimit(double temperature, int stages) {
        return temperature > 0 && temperature < 9 ? Math.min(stages, 1 + (int) (temperature / 3)) : 0;
    }

    public static double heatingBudgetJ() {
        var edge = MinecraftThermalProfiles.materialLaw(FHBlocks.THIN_ICE_BLOCK.get().defaultBlockState()).heating();
        return (edge.targetEnthalpyJ() - edge.sourceEnthalpyJ()) / 4;
    }

    public static double coolingBudgetJ() {
        var edge = MinecraftThermalProfiles.materialLaw(Blocks.WATER.defaultBlockState()).cooling();
        return (edge.sourceEnthalpyJ() - edge.targetEnthalpyJ()) / 4;
    }

    public static void observe(ServerLevel level, BlockPos pos, BlockState state) {
        var ice = (ThinIceBlock) state.getBlock();
        double temperature = temperature(level, pos);
        if (!Double.isFinite(temperature)) return;
        if (warmLimit(temperature, ice.fractureStages(state)) <= state.getValue(ThinIceBlock.CRACK)
                && !(temperature < WorldTemperature.WATER_FREEZES && state.getValue(ThinIceBlock.CRACK) > 0
                && ThinIceBlock.hasAdjacentWater(level, pos))) return;
        var data = get(level);
        var entry = data.progress.get(pos.asLong());
        if (entry == null || entry.invalid) {
            data.progress.put(pos.asLong(), new Progress(0));
            data.setDirty();
        }
        data.active.add(pos.asLong());
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % INTERVAL != 0) return;
        var data = level.getDataStorage().get(ThinIcePhaseData::load, ID);
        if (data == null || data.active.isEmpty()) return;
        if (level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING) <= 0) return;
        double capacity = MinecraftThermalProfiles.materialLaw(FHBlocks.THIN_ICE_BLOCK.get().defaultBlockState()).capacityJPerK();
        double conductance = DormantThermalCooling.rate(MinecraftThermalProfiles.dormantTemperatureHalfLifeSeconds()) * capacity;
        double heatingBudget = heatingBudgetJ(), coolingBudget = coolingBudgetJ();
        var iterator = data.active.iterator();
        while (iterator.hasNext()) {
            long key = iterator.nextLong();
            var pos = BlockPos.of(key);
            var value = data.progress.get(key);
            if (value == null || value.invalid) {
                iterator.remove();
                data.setDirty();
                continue;
            }
            // Unloaded positions retain progress but accrue no off-screen work.
            if (!level.hasChunkAt(pos) || !level.isPositionEntityTicking(pos)) {
                iterator.remove();
                continue;
            }
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof ThinIceBlock ice)) {
                iterator.remove();
                data.progress.remove(key);
                data.setDirty();
                continue;
            }
            double temperature = temperature(level, pos);
            if (!Double.isFinite(temperature)) {
                iterator.remove();
                continue;
            }
            int crack = state.getValue(ThinIceBlock.CRACK);
            int limit = warmLimit(temperature, ice.fractureStages(state));
            if (crack < limit) {
                value.workJ += conductance * temperature * (INTERVAL / 20.0);
                int amount = Math.min(limit - crack, (int) Math.floor(Math.max(0, value.workJ) / heatingBudget));
                if (amount > 0) {
                    value.workJ -= amount * heatingBudget;
                    ice.advanceCrack(level, pos, amount);
                }
                data.setDirty();
            } else if (crack > 0 && temperature < WorldTemperature.WATER_FREEZES
                    && ThinIceBlock.hasAdjacentWater(level, pos)
                    && level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(pos.getX(), pos.getY() + state.getCollisionShape(level, pos).max(net.minecraft.core.Direction.Axis.Y) - 0.01,
                            pos.getZ(), pos.getX() + 1, pos.getY() + 1.1, pos.getZ() + 1),
                    ThinIceBlock::canCrack).isEmpty()) {
                value.workJ -= conductance * (WorldTemperature.WATER_FREEZES - temperature) * (INTERVAL / 20.0);
                int amount = Math.min(crack, (int) Math.floor(Math.max(0, -value.workJ) / coolingBudget));
                if (amount > 0) {
                    value.workJ += amount * coolingBudget;
                    level.setBlockAndUpdate(pos, state.setValue(ThinIceBlock.CRACK, crack - amount));
                    level.sendParticles(ParticleTypes.SNOWFLAKE, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 2, 0.2, 0.05, 0.2, 0);
                }
                data.setDirty();
            } else {
                iterator.remove();
                if (value.workJ == 0) data.progress.remove(key);
                data.setDirty();
                continue;
            }
            if (value.invalid || !(level.getBlockState(pos).getBlock() instanceof ThinIceBlock)) {
                iterator.remove();
                data.setDirty();
            }
        }
    }
}

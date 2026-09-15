/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft;

import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticField;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalAnalyticFieldIndex;
import com.teammoeg.frostedheart.content.climate.thermal.field.ThermalFieldKey;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.IdentityHashMap;

/** World-lifetime main-thread fields; physical worker restarts do not invalidate them. */
public final class MinecraftGameplayFields {
    private static final IdentityHashMap<ServerLevel, ThermalAnalyticFieldIndex> LEVELS = new IdentityHashMap<>();
    private static final ThermalAnalyticFieldIndex.Sample FLOOR_SAMPLE = new ThermalAnalyticFieldIndex.Sample();

    private MinecraftGameplayFields() {}

    static ThermalAnalyticFieldIndex indexFor(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, ignored -> new ThermalAnalyticFieldIndex());
    }

    static ThermalAnalyticFieldIndex existing(ServerLevel level) {
        return LEVELS.get(level);
    }

    /** Main-thread phase queries use explicit floors; additive fields alone cannot force a phase. */
    public static double guaranteedFloor(ServerLevel level, BlockPos position) {
        var fields = existing(level);
        if (fields == null) return Double.NEGATIVE_INFINITY;
        fields.sample(position.getX() + .5, position.getY() + .5, position.getZ() + .5, FLOOR_SAMPLE);
        if (!FLOOR_SAMPLE.present()) return Double.NEGATIVE_INFINITY;
        double natural = FLOOR_SAMPLE.requiresNatural() ? WorldTemperature.naturalBlock(level, position) : 0;
        return FLOOR_SAMPLE.guaranteedFloor(natural);
    }

    public static boolean upsert(ServerLevel level, ThermalAnalyticField field) {
        if (!level.getServer().isSameThread()) return false;
        indexFor(level).upsert(field);
        return true;
    }

    public static void upsertSphere(ServerLevel level, ThermalFieldKey key, int priority,
            ThermalAnalyticField.CombineMode mode, double x, double y, double z, double radius, double value) {
        if (level.getServer().isSameThread()) {
            indexFor(level).upsertSphere(key, priority, mode, x, y, z, radius, value);
        }
    }

    public static boolean remove(ServerLevel level, ThermalFieldKey key) {
        if (!level.getServer().isSameThread()) return false;
        ThermalAnalyticFieldIndex index = existing(level);
        return index != null && index.remove(key);
    }

    public static void beginProviderRefresh(ServerLevel level, ResourceLocation provider) {
        ThermalAnalyticFieldIndex index = existing(level);
        if (index != null) index.beginProviderRefresh(provider);
    }

    public static void endProviderRefresh(ServerLevel level, ResourceLocation provider) {
        ThermalAnalyticFieldIndex index = existing(level);
        if (index != null) index.endProviderRefresh(provider);
    }

    public static void unload(ServerLevel level) { LEVELS.remove(level); }
    public static void stop() { LEVELS.clear(); }
}

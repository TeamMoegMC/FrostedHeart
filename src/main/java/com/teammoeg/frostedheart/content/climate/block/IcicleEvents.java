/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** Explicit vibration sources only; no listeners, world scan, or per-tick acoustic simulation. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IcicleEvents {
    private record Candidate(BlockPos pos, BlockState before) {}
    private IcicleEvents() {}

    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) vibrate(level, event.getExplosion().getPosition(), 32, level.random);
    }

    @SubscribeEvent public static void bell(PlayLevelSoundEvent.AtPosition event) {
        if (event.getLevel() instanceof ServerLevel level && event.getSound() != null
                && event.getSound().value() == SoundEvents.BELL_BLOCK
                && level.getBlockState(BlockPos.containing(event.getPosition())).getBlock() instanceof BellBlock)
            vibrate(level, event.getPosition(), 12, level.random);
    }

    public static void vibrate(ServerLevel level, Vec3 center, int radius, RandomSource random) {
        int minX = Mth.floor(center.x - radius), maxX = Mth.floor(center.x + radius);
        int minY = Math.max(level.getMinBuildHeight(), Mth.floor(center.y - radius));
        int maxY = Math.min(level.getMaxBuildHeight() - 1, Mth.floor(center.y + radius));
        int minZ = Mth.floor(center.z - radius), maxZ = Mth.floor(center.z + radius);
        List<Candidate> candidates = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++) for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
            var chunk = level.getChunkSource().getChunkNow(cx, cz);
            if (chunk == null) continue;
            for (int sy = minY >> 4; sy <= maxY >> 4; sy++) {
                var section = chunk.getSection(chunk.getSectionIndexFromSectionY(sy));
                if (!section.getStates().maybeHas(IceDripping::isHangingIce)) continue;
                for (int x = Math.max(minX, cx << 4); x <= Math.min(maxX, (cx << 4) + 15); x++)
                    for (int z = Math.max(minZ, cz << 4); z <= Math.min(maxZ, (cz << 4) + 15); z++)
                        for (int y = Math.max(minY, sy << 4); y <= Math.min(maxY, (sy << 4) + 15); y++) {
                            BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                            if (!IceDripping.isHangingIce(state)) continue;
                            cursor.set(x, y, z);
                            if (cursor.distToCenterSqr(center) <= radius * radius)
                                candidates.add(new Candidate(cursor.immutable(), state));
                        }
            }
        }
        // Snapshot warning state before mutations: a newly warned segment gets no
        // second 90% roll from this same event, even if neighbors change its shape.
        for (Candidate candidate : candidates) {
            if (EaveIcicleBlock.isEaveIce(candidate.before)) {
                if (random.nextDouble() < .05) EaveIcicleBlock.fall(level, candidate.pos);
                continue;
            }
            boolean cracked = candidate.before.getValue(IcicleBlock.CRACKED);
            if (random.nextDouble() >= (cracked ? .9 : .05)) continue;
            BlockState current = level.getBlockState(candidate.pos);
            if (!(current.getBlock() instanceof IcicleBlock icicle)) continue;
            if (cracked) icicle.shatter(level, candidate.pos);
            else icicle.warn(level, candidate.pos, random);
        }
    }
}

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.Tags;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Explicit event sources only; no per-tick acoustic simulation. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IcicleEvents {
    public record Vibration(int radius, double crackChance, double shatterChance) {}
    public static final Vibration EXPLOSION = new Vibration(32, .10, 1);
    public static final Vibration BELL = new Vibration(12, .01, .50);
    public static final Vibration ICE_BREAK = new Vibration(16, .004, .50);
    public static final Vibration STONE_BREAK = new Vibration(8, .002, .50);
    public static final Vibration FIREWORK = new Vibration(24, .02, 1);
    private static final Set<SoundType> STONE_SOUNDS = Set.of(SoundType.STONE, SoundType.DEEPSLATE,
            SoundType.DEEPSLATE_BRICKS, SoundType.DEEPSLATE_TILES, SoundType.POLISHED_DEEPSLATE,
            SoundType.NETHERRACK, SoundType.NETHER_ORE, SoundType.GILDED_BLACKSTONE,
            SoundType.BASALT, SoundType.TUFF, SoundType.CALCITE, SoundType.DRIPSTONE_BLOCK);
    private record Candidate(BlockPos pos, BlockState before) {}
    private IcicleEvents() {}

    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) vibrate(level, event.getExplosion().getPosition(), EXPLOSION, level.random);
    }

    @SubscribeEvent public static void bell(PlayLevelSoundEvent.AtPosition event) {
        if (event.getLevel() instanceof ServerLevel level && event.getSound() != null
                && event.getSound().value() == SoundEvents.BELL_BLOCK
                && level.getBlockState(BlockPos.containing(event.getPosition())).getBlock() instanceof BellBlock)
            vibrate(level, event.getPosition(), BELL, level.random);
    }

    /** Level events carry the destroyed state even when the block has already gone. */
    public static void levelEvent(ServerLevel level, int event, BlockPos pos, int data) {
        Vibration vibration;
        if (event == 1029 || event == 1030 || event == 1031) vibration = BELL; // Anvil break/use/land.
        else if (event == 2001) {
            BlockState state = Block.stateById(data);
            // Shattering/falling hanging ice also emits 2001; never recurse into it.
            if (IceDripping.isHangingIce(state)) return;
            if (state.is(BlockTags.ICE)) vibration = ICE_BREAK;
            else {
                SoundType sound = state.getSoundType(level, pos, null);
                if (sound == SoundType.ANVIL) vibration = BELL;
                else if (state.is(Tags.Blocks.ORES) || STONE_SOUNDS.contains(sound)) vibration = STONE_BREAK;
                else return;
            }
        } else return;
        vibrate(level, Vec3.atCenterOf(pos), vibration, level.random);
    }

    /** The burst packet is sent once per rocket; individual client spark sounds do not retrigger it. */
    public static void fireworkBurst(ServerLevel level, FireworkRocketEntity rocket) {
        var fireworks = rocket.getItem().getTagElement("Fireworks");
        if (fireworks != null && !fireworks.getList("Explosions", 10).isEmpty())
            vibrate(level, rocket.position(), FIREWORK, level.random);
    }

    public static void vibrate(ServerLevel level, Vec3 center, Vibration vibration, RandomSource random) {
        int radius = vibration.radius;
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
        // second shatter roll from this same event, even if neighbors change its shape.
        for (Candidate candidate : candidates) {
            double attenuation = Math.max(0, 1 - Math.sqrt(candidate.pos.distToCenterSqr(center)) / radius);
            if (EaveIcicleBlock.isEaveIce(candidate.before)) {
                if (random.nextDouble() < vibration.crackChance * attenuation) EaveIcicleBlock.fall(level, candidate.pos);
                continue;
            }
            boolean cracked = candidate.before.getValue(IcicleBlock.CRACKED);
            if (random.nextDouble() >= (cracked ? vibration.shatterChance : vibration.crackChance) * attenuation) continue;
            BlockState current = level.getBlockState(candidate.pos);
            if (!(current.getBlock() instanceof IcicleBlock icicle)) continue;
            if (cracked) icicle.shatter(level, candidate.pos);
            else icicle.warn(level, candidate.pos, random);
        }
    }
}

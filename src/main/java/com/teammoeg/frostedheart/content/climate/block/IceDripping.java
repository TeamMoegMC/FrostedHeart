/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.bootstrap.common.FHMobEffects;
import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData.BodyPart;
import com.teammoeg.frostedheart.content.climate.thermal.runtime.minecraft.input.MinecraftPhaseController;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Nearby loaded ice only: no timers per block, droplet entities, or thermal writes. */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class IceDripping {
    public static final double MELTING_C = 2;
    public static final int RANGE = 16, WET_TICKS = 100;
    private static final int VIEW_RADIUS = 8;
    private IceDripping() {}

    public static boolean isHangingIce(BlockState state) {
        return IcicleBlock.isIcicle(state) || EaveIcicleBlock.isEaveIce(state);
    }

    public static boolean warmEnough(double temperature) { return Double.isFinite(temperature) && temperature >= MELTING_C; }

    public static boolean melting(ServerLevel level, BlockPos outlet) {
        BlockState state = level.getBlockState(outlet);
        if (!isHangingIce(state)) return false;
        BlockPos.MutableBlockPos cursor = outlet.mutable();
        // A cold terminal may drain a warm upper segment. No nonlocal energy transfer.
        do {
            if (warmEnough(MinecraftPhaseController.temperature(level, cursor))) return true;
            if (!IcicleBlock.isIcicle(state)) return false;
            cursor.move(Direction.UP);
            if (level.isOutsideBuildHeight(cursor)) return false;
            state = level.getBlockState(cursor);
        } while (IcicleBlock.isIcicle(state));
        return false;
    }

    @SubscribeEvent public static void tick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || level.getGameTime() % 20 != 0 || level.players().isEmpty()) return;
        LongOpenHashSet visited = new LongOpenHashSet();
        for (var viewer : level.players()) {
            if (viewer.isSpectator()) continue;
            int minX = viewer.getBlockX() - VIEW_RADIUS, maxX = viewer.getBlockX() + VIEW_RADIUS;
            int minZ = viewer.getBlockZ() - VIEW_RADIUS, maxZ = viewer.getBlockZ() + VIEW_RADIUS;
            int minY = Math.max(level.getMinBuildHeight(), viewer.getBlockY() - 4);
            int maxY = Math.min(level.getMaxBuildHeight() - 1, net.minecraft.util.Mth.floor(viewer.getEyeY()) + RANGE);
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int cx = minX >> 4; cx <= maxX >> 4; cx++) for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++) {
                var chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (int sy = minY >> 4; sy <= maxY >> 4; sy++) {
                    var section = chunk.getSection(chunk.getSectionIndexFromSectionY(sy));
                    if (!section.getStates().maybeHas(IceDripping::isHangingIce)) continue;
                    for (int y = Math.max(minY, sy << 4); y <= Math.min(maxY, (sy << 4) + 15); y++)
                        for (int x = Math.max(minX, cx << 4); x <= Math.min(maxX, (cx << 4) + 15); x++)
                            for (int z = Math.max(minZ, cz << 4); z <= Math.min(maxZ, (cz << 4) + 15); z++) {
                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                if (!isHangingIce(state)) continue;
                                cursor.set(x, y, z);
                                if (IcicleBlock.isIcicle(state) && IcicleBlock.isIcicle(level.getBlockState(cursor.below()))) continue;
                                if (!visited.add(cursor.asLong()) || !level.random.nextBoolean() || !melting(level, cursor)) continue;
                                Vec3 start = outletPosition(state, cursor, level.random.nextDouble());
                                level.sendParticles(ParticleTypes.FALLING_DRIPSTONE_WATER, start.x, start.y, start.z, 1, 0, 0, 0, 0);
                                wetBelow(level, start);
                            }
                }
            }
        }
    }

    public static Vec3 outletPosition(BlockState state, BlockPos pos, double choice) {
        if (!EaveIcicleBlock.isEaveIce(state)) return new Vec3(pos.getX() + .5, pos.getY() + .48, pos.getZ() + .5);
        int mask = EaveIcicleBlock.mask(state), count = Integer.bitCount(mask);
        if (count == 0) return Vec3.atBottomCenterOf(pos);
        int selected = Math.min(count - 1, (int) (choice * count));
        Direction edge = Direction.NORTH;
        for (Direction candidate : EaveIcicleBlock.EDGES) {
            if (!state.getValue(EaveIcicleBlock.property(candidate))) continue;
            if (selected-- == 0) { edge = candidate; break; }
        }
        return new Vec3(pos.getX() + .5 + .4 * edge.getStepX(), pos.getY() + .29, pos.getZ() + .5 + .4 * edge.getStepZ());
    }

    /** Drops are a low-frequency vertical hit approximation; solid collision shapes stop them. */
    public static void wetBelow(ServerLevel level, Vec3 start) {
        Vec3 stop = endOfDrop(level, start);
        Player first = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (var player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.getEyeY() > start.y) continue;
            var intersection = player.getBoundingBox().clip(start, stop);
            if (intersection.isEmpty()) continue;
            double distance = start.distanceToSqr(intersection.get());
            if (distance < nearest) { nearest = distance; first = player; }
        }
        if (first != null) wet(first);
    }

    public static Vec3 endOfDrop(ServerLevel level, Vec3 start) {
        Vec3 end = start.add(0, -RANGE, 0);
        // The entire ray stays in the outlet's already loaded column.
        var hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, null));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    public static boolean waterproofHead(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.getItem() instanceof ArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.HEAD || waterproofHat(helmet)) return true;
        var temperature = PlayerTemperatureData.getCapability(player);
        if (temperature.isPresent()) {
            var hats = temperature.orElseThrow(IllegalStateException::new).getClothesByPart(BodyPart.HEAD);
            for (int i = 0; i < hats.getSlots(); i++) if (waterproofHat(hats.getStackInSlot(i))) return true;
        }
        return false;
    }

    private static boolean waterproofHat(ItemStack stack) {
        ArmorTempData data = stack.isEmpty() ? null : ArmorTempData.getData(stack, BodyPart.HEAD);
        return data != null && data.getFluidResistance() > 0;
    }

    public static void wet(Player player) {
        if (waterproofHead(player)) return;
        var current = player.getEffect(FHMobEffects.WET.get());
        if (current == null || current.getDuration() < WET_TICKS)
            player.addEffect(new MobEffectInstance(FHMobEffects.WET.get(), WET_TICKS, 0, false, false));
    }
}

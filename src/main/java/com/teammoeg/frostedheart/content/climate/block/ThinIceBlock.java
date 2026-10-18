/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.content.climate.block;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import com.teammoeg.frostedheart.bootstrap.reference.FHSoundEvents;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundSource;

public class ThinIceBlock extends IceBlock {
    public static final IntegerProperty CRACK = IntegerProperty.create("crack", 0, 3);

    public ThinIceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CRACK, 0));
    }

    public int fractureStages(BlockState state) {
        return 4;
    }

    public static boolean canCrack(Entity entity) {
        return entity instanceof LivingEntity
                && !(entity instanceof Player player && player.isSpectator());
    }

    public static int contactInterval(Entity entity) {
        if (entity.isCrouching()) return 96;
        double dx = entity.getX() - entity.xo, dz = entity.getZ() - entity.zo;
        if (dx * dx + dz * dz < 1.0E-6) return 24;
        return entity.isSprinting() ? 6 : 12;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRACK);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        // Vanilla's 0.2-block step sample can point below a single layer of ice.
        if (level instanceof ServerLevel server && canCrack(entity) && pos.equals(entity.getOnPos()))
            ThinIceEvents.contact(server, pos, contactInterval(entity));
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float distance) {
        if (level instanceof ServerLevel server && canCrack(entity))
            impact(server, pos, distance);
        super.fallOn(level, state, pos, entity, distance);
    }

    public static int impactDamage(float distance, RandomSource random) {
        if (distance < 1) return 0;
        double damage = Math.min(4, distance / 2.0);
        int whole = (int) damage;
        return whole + (random.nextFloat() < damage - whole ? 1 : 0);
    }

    public void impact(ServerLevel level, BlockPos pos, float distance) {
        if (distance < 1 || !level.getBlockState(pos).is(this)) return;
        int damage = impactDamage(distance, level.random);
        if (damage == 0 || advanceCrack(level, pos, damage))
            startCooldown(level, pos, 24);
    }

    public void ordinaryContact(ServerLevel level, BlockPos pos, int interval) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)
                && level.random.nextInt(interval) == 0
                && advanceCrack(level, pos, 1))
            startCooldown(level, pos, interval);
    }

    private void startCooldown(ServerLevel level, BlockPos pos, int ticks) {
        // Replace a previous cooldown so an impact always starts a full 24 ticks.
        level.getBlockTicks().clearArea(new net.minecraft.world.level.levelgen.structure.BoundingBox(
                pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()));
        level.scheduleTick(pos, this, ticks);
    }

    /** Returns true if the ice remains, so callers can install a cooldown. */
    public boolean advanceCrack(ServerLevel level, BlockPos pos, int amount) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(this)) return false;
        int crack = state.getValue(CRACK) + amount;
        if (crack >= fractureStages(state)) {
            boolean water = hasAdjacentWater(level, pos);
            level.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(state));
            level.setBlockAndUpdate(pos, water ? net.minecraft.world.level.block.Blocks.WATER.defaultBlockState()
                    : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.gameEvent(net.minecraft.world.level.gameevent.GameEvent.BLOCK_DESTROY, pos,
                    net.minecraft.world.level.gameevent.GameEvent.Context.of(state));
            return false;
        }
        level.setBlockAndUpdate(pos, state.setValue(CRACK, crack));
        float volume = crack == 3 ? 0.9F : 0.15F;
        level.playSound(null, pos, FHSoundEvents.ICE_CRACKING.get(), SoundSource.BLOCKS, volume, 1.0F);
        return true;
    }

    public static boolean hasAdjacentWater(Level level, BlockPos pos) {
        BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();
        for (Direction direction : Direction.values()) {
            neighbor.setWithOffset(pos, direction);
            if (level.hasChunkAt(neighbor) && level.getBlockState(neighbor).is(net.minecraft.world.level.block.Blocks.WATER))
                return true;
        }
        return false;
    }

    // Scheduled ticks are cooldown tokens; their expiry does not advance cracks.
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
    }

    @Override
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        ThinIcePhaseData.observe(pLevel, pPos, pState);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level instanceof ServerLevel server)
            ThinIcePhaseData.discard(server, pos);
        super.onRemove(state, level, pos, next, moving);
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState adj, Direction side) {
        if (adj.is(this)) {
                return true;
        }
        if (adj.is(FHBlocks.LAYERED_THIN_ICE.get()) && adj.hasProperty(LayeredThinIceBlock.LAYERS)) {
            return adj.getValue(LayeredThinIceBlock.LAYERS).equals(LayeredThinIceBlock.MAX_HEIGHT);
        }
        return super.skipRendering(state, adj, side);
    }
}

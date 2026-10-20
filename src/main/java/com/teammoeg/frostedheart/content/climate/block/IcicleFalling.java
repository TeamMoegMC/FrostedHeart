/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.content.climate.block;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.AABB;

/** Uses vanilla falling-block tracking, rendering and NBT; ice never becomes a placed block. */
public final class IcicleFalling {
    private IcicleFalling() {}

    public static void tick(FallingBlockEntity entity) {
        Level level = entity.level();
        entity.time++;
        if (!entity.isNoGravity()) entity.setDeltaMovement(entity.getDeltaMovement().add(0, -.04, 0));
        AABB before = entity.getBoundingBox();
        entity.move(MoverType.SELF, entity.getDeltaMovement());
        if (!level.isClientSide) {
            AABB swept = before.minmax(entity.getBoundingBox());
            boolean hit = false;
            if (EaveIcicleBlock.isEaveIce(entity.getBlockState())) {
                var targets = level.getEntities(entity, swept,
                        EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(EntitySelector.LIVING_ENTITY_STILL_ALIVE));
                var target = targets.stream().min(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(entity))).orElse(null);
                if (target != null) {
                    target.hurt(entity.damageSources().fallingStalactite(entity), .5F);
                    hit = true;
                }
            }
            boolean water = false;
            for (BlockPos pos : BlockPos.betweenClosed(net.minecraft.util.Mth.floor(swept.minX), net.minecraft.util.Mth.floor(swept.minY),
                    net.minecraft.util.Mth.floor(swept.minZ), net.minecraft.util.Mth.floor(swept.maxX),
                    net.minecraft.util.Mth.floor(swept.maxY), net.minecraft.util.Mth.floor(swept.maxZ))) {
                if (!level.hasChunkAt(pos)) continue;
                water |= level.getFluidState(pos).is(FluidTags.WATER);
                extinguish(level, pos);
            }
            if (entity.onGround()) extinguish(level, entity.blockPosition().below());
            if (hit || water || entity.onGround() || entity.verticalCollision || entity.time > 600
                    || entity.getY() < level.getMinBuildHeight() - 1) {
                level.levelEvent(2001, entity.blockPosition(), Block.getId(entity.getBlockState()));
                entity.discard();
                return;
            }
        }
        entity.setDeltaMovement(entity.getDeltaMovement().scale(.98));
    }

    public static boolean extinguish(Level level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if ((state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)) && state.getValue(CampfireBlock.LIT)) {
            CampfireBlock.dowse(null, level, pos, state);
            return level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
        }
        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            level.levelEvent(1009, pos, 0);
            return level.removeBlock(pos, false);
        }
        return false;
    }

    public static float damage(float distance, float perDistance, int maximum) {
        return Math.min(net.minecraft.util.Mth.floor(Math.max(0, net.minecraft.util.Mth.ceil(distance - 1)) * perDistance), maximum) / 3F;
    }
}

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

package com.teammoeg.frostedheart.item;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * 专门用于破坏雪的工具，有耐久度（会损坏）
 */
public class SnowBreakerItem extends FHBaseItem {
    /**
     * 破坏雪时是否正常掉落雪球
     */
    public final boolean dropItem;

    public SnowBreakerItem(Boolean dropItem, Properties builder) {
        super(builder);
        this.dropItem = dropItem;

    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        var player = context.getPlayer();
        if (context.getHand() == InteractionHand.MAIN_HAND && state.is(BlockTags.SNOW)) {
            if (player instanceof ServerPlayer sp) {
                BlockPos.betweenClosedStream(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))
                        .forEach(pos2 -> {
                            if (level.getBlockState(pos2).is(BlockTags.SNOW)) {
                                sp.gameMode.destroyBlock(pos2);
                            }
                        });
                if (!player.isCreative()) {
                    player.getCooldowns().addCooldown(context.getItemInHand().getItem(), 10);
                }
                level.playSound(player, pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS);
                return InteractionResult.SUCCESS;
            } else if (level instanceof ClientLevel l) {
                BlockPos.betweenClosedStream(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))
                        .forEach(pos2 -> {
                            var s = level.getBlockState(pos2);
                            if (s.is(BlockTags.SNOW)) {
                                l.addDestroyBlockEffect(pos2, s);
                            }
                        });
                l.playLocalSound(pos, state.getSoundType().getBreakSound(), SoundSource.BLOCKS, 1, 1, true);
            }
        }
        return super.useOn(context);
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack pStack, BlockState pState) {
        if (pState.is(BlockTags.SNOW)) {
            return 1024.0F;
        } else {
            return 0.0F;
        }
    }

    @Override
    public boolean mineBlock(@NotNull ItemStack stack, @NotNull Level world, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull LivingEntity entityLiving) {
        if (state.is(BlockTags.SNOW)) {
            // 挖掘雪块时消耗耐久
            stack.hurtAndBreak(1, entityLiving, (player) -> player.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        return true;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return this.dropItem && state.is(BlockTags.SNOW);
    }
}

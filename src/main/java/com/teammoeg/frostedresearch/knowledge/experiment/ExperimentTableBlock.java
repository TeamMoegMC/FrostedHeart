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

/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedresearch.knowledge.experiment;

import com.teammoeg.chorda.block.CBlock;
import com.teammoeg.chorda.block.CEntityBlock;
import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.util.CUtils;
import com.teammoeg.frostedresearch.FRContents;
import com.teammoeg.frostedresearch.Lang;
import com.teammoeg.frostedresearch.mixinutil.IOwnerTile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 实验台方块：首次使用时归属当前队伍，之后只对该队伍打开。 / Experiment table, owned by the first team to use it.
 */
public class ExperimentTableBlock extends CBlock implements CEntityBlock<ExperimentTableBlockEntity> {
    public ExperimentTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Supplier<BlockEntityType<ExperimentTableBlockEntity>> getBlock() {
        return FRContents.BlockEntityTypes.EXPERIMENT_TABLE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND && !player.isShiftKeyDown()
                && CUtils.getExistingTileEntity(level, pos) instanceof ExperimentTableBlockEntity table) {
            UUID team = CTeamDataManager.get(player).getId();
            IOwnerTile.trySetOwner(table, team);
            if (team != null && team.equals(IOwnerTile.getOwner(table)))
                NetworkHooks.openScreen((ServerPlayer) player, table, pos);
            else
                player.displayClientMessage(Lang.translateMessage("research.not_owned"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (level.getBlockEntity(pos) instanceof ExperimentTableBlockEntity table && state.getBlock() != newState.getBlock()) {
            for (int i = 0; i < table.getInventory().getSlots(); i++) {
                ItemStack stack = table.getInventory().getStackInSlot(i);
                table.getInventory().setStackInSlot(i, ItemStack.EMPTY);
                if (!stack.isEmpty()) popResource(level, pos, stack);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}

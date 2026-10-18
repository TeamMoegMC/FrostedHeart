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

package com.teammoeg.frostedheart.content.town.buildings.logistics;

import java.util.function.Supplier;

import com.teammoeg.chorda.block.CEntityBlock;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.block.AbstractTownBuildingBlock;
import com.teammoeg.frostedheart.util.CConstants;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/** Core block for a town transport station. */
public class TransportStationBlock extends AbstractTownBuildingBlock
        implements CEntityBlock<TransportStationBlockEntity> {
    public TransportStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (level.isClientSide || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof TransportStationBlockEntity blockEntity)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }
        blockEntity.getBuilding().ifPresentOrElse(building -> {
            blockEntity.refresh_safe(building);
            NetworkHooks.openScreen(serverPlayer, blockEntity, pos);
        }, () -> player.displayClientMessage(
                Components.str(CConstants.NO_CORRESPONDING_TOWN_BUILDING_INSTANCE_FOUND), false));
        return InteractionResult.SUCCESS;
    }

    @Override
    public Supplier<BlockEntityType<TransportStationBlockEntity>> getBlock() {
        return FHBlockEntityTypes.TRANSPORT_STATION;
    }
}

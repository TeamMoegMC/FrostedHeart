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

package com.teammoeg.frostedheart.content.town.buildings.warehouse;

import com.teammoeg.chorda.block.CGuiBlock;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A full wall block that bridges a warehouse's virtual item storage with
 * automation-facing physical buffer slots.
 */
public class WarehouseInterfaceBlock extends CGuiBlock<WarehouseInterfaceBlockEntity> {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<TransportVisualState> TRANSPORT_STATE =
            EnumProperty.create("transport_state", TransportVisualState.class);

    public WarehouseInterfaceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TRANSPORT_STATE, TransportVisualState.UNAVAILABLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, TRANSPORT_STATE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof ServerPlayer player
                && level.getBlockEntity(pos) instanceof WarehouseInterfaceBlockEntity warehouseInterface) {
            warehouseInterface.setAdmissionNoticePlayer(player);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block block,
                                BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving);
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof WarehouseInterfaceBlockEntity warehouseInterface) {
            warehouseInterface.onNeighborSignalChanged();
        }
    }

    @Override
    public Supplier<BlockEntityType<WarehouseInterfaceBlockEntity>> getBlock() {
        return FHBlockEntityTypes.WAREHOUSE_INTERFACE;
    }

    static BlockState withTransportVisualState(
            BlockState current,
            WarehouseInterfaceTransportStatus status
    ) {
        if (current == null || !current.hasProperty(TRANSPORT_STATE)) {
            return current;
        }
        TransportVisualState next = TransportVisualState.from(status);
        return !shouldUpdateTransportVisualState(current.getValue(TRANSPORT_STATE), next)
                ? current
                : current.setValue(TRANSPORT_STATE, next);
    }

    static boolean shouldUpdateTransportVisualState(
            TransportVisualState current,
            TransportVisualState next
    ) {
        return current != next;
    }

    public enum TransportVisualState implements StringRepresentable {
        ACTIVE("active"),
        DISABLED("disabled"),
        SHORTAGE("shortage"),
        UNAVAILABLE("unavailable");

        private final String serializedName;

        TransportVisualState(String serializedName) {
            this.serializedName = serializedName;
        }

        public static TransportVisualState from(WarehouseInterfaceTransportStatus status) {
            return switch (status) {
                case ACTIVE -> ACTIVE;
                case DISABLED -> DISABLED;
                case THROTTLED -> SHORTAGE;
                case UNBOUND, WAREHOUSE_UNAVAILABLE -> UNAVAILABLE;
            };
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}

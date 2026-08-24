/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.town.transport.device;

import com.teammoeg.chorda.block.CGuiBlock;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.transport.P2PTerminalRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/** One-block freight terminal; role controls direction, shape, and server capabilities. */
public final class P2PTerminalBlock extends CGuiBlock<P2PTerminalBlockEntity> {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<P2PTerminalVisualState> VISUAL_STATE =
            EnumProperty.create("visual_state", P2PTerminalVisualState.class);

    private static final VoxelShape BODY = Block.box(1, 0, 1, 15, 14, 15);
    private final P2PTerminalRole role;

    public P2PTerminalBlock(Properties properties, P2PTerminalRole role) {
        super(properties);
        this.role = Objects.requireNonNull(role, "role");
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(VISUAL_STATE, P2PTerminalVisualState.UNBOUND));
    }

    public P2PTerminalRole role() {
        return role;
    }

    /** The only side where shipping/receiving terminals may contact a local inventory. */
    public Direction inventoryConnectionFace(BlockState state) {
        return inventoryConnectionFace(state.getValue(FACING));
    }

    static Direction inventoryConnectionFace(Direction front) {
        return front.getOpposite();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, VISUAL_STATE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING,
                context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof ServerPlayer player
                && level.getBlockEntity(pos) instanceof P2PTerminalBlockEntity terminal) {
            terminal.claimOrAuthorize(player);
        }
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
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level.getBlockEntity(pos) instanceof P2PTerminalBlockEntity terminal)
                || !terminal.claimOrAuthorize(serverPlayer)) {
            return InteractionResult.CONSUME;
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighbor,
            BlockPos fromPos,
            boolean isMoving
    ) {
        super.neighborChanged(state, level, pos, neighbor, fromPos, isMoving);
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof P2PTerminalBlockEntity terminal) {
            terminal.onNeighborFactChanged();
        }
    }

    @Override
    public VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        Direction facing = state.getValue(FACING);
        return switch (role) {
            case SHIPPING -> BODY;
            case RECEIVING -> Shapes.or(BODY, frontPort(facing, 4, 12));
            case BIDIRECTIONAL -> Shapes.or(BODY, frontPort(facing, 6, 10),
                    sideMeter(facing));
        };
    }

    private static VoxelShape frontPort(Direction facing, int from, int to) {
        return switch (facing) {
            case NORTH -> Block.box(from, 4, 0, to, 12, 2);
            case SOUTH -> Block.box(from, 4, 14, to, 12, 16);
            case WEST -> Block.box(0, 4, from, 2, 12, to);
            case EAST -> Block.box(14, 4, from, 16, 12, to);
            case UP -> Block.box(from, 14, 4, to, 16, 12);
            case DOWN -> Block.box(from, 0, 4, to, 2, 12);
        };
    }

    private static VoxelShape sideMeter(Direction facing) {
        return switch (facing) {
            case NORTH, SOUTH -> Block.box(0, 7, 5, 2, 13, 11);
            case WEST, EAST -> Block.box(5, 7, 0, 11, 13, 2);
            case UP, DOWN -> Block.box(0, 5, 5, 2, 11, 11);
        };
    }

    @Override
    public Supplier<BlockEntityType<P2PTerminalBlockEntity>> getBlock() {
        return FHBlockEntityTypes.P2P_TERMINAL;
    }
}

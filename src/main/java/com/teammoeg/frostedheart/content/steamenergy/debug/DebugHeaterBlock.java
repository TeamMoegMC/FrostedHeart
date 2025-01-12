/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.steamenergy.debug;

import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.base.block.FHEntityBlock;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.lang.Lang;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

public class DebugHeaterBlock extends FHBaseBlock implements FHEntityBlock<DebugHeaterTileEntity> {
    public DebugHeaterBlock(Properties blockProps) {
        super(blockProps);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.LEVEL_FLOWING);
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        InteractionResult superResult = super.use(state, world, pos, player, hand, hit);
        if (superResult.consumesAction() || player.isShiftKeyDown())
            return superResult;
        ItemStack item = player.getItemInHand(hand);
        if (item.getItem().equals(Item.byBlock(this))) {
            state = state.cycle(BlockStateProperties.LEVEL_FLOWING);
            world.setBlockAndUpdate(pos, state);
            player.displayClientMessage(Lang.str(String.valueOf(state.getValue(BlockStateProperties.LEVEL_FLOWING))), true);
            return InteractionResult.SUCCESS;
        }
        return superResult;
    }

    @Override
    public Supplier<BlockEntityType<DebugHeaterTileEntity>> getBlock() {
        return FHBlockEntityTypes.DEBUGHEATER;
    }

    @Override
    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
                                boolean isMoving) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
        //Direction d = FHUtils.dirBetween(fromPos, pos);
        //System.out.println("changed")2
        if(!worldIn.isClientSide)
            worldIn.scheduleTick(pos, this, 10);

    }

    @Override
    public void tick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource pRandom) {
        //System.out.println("ticked "+pos);
        super.tick(state, worldIn, pos, pRandom);
        DebugHeaterTileEntity te=FHUtils.getExistingTileEntity(worldIn, pos, DebugHeaterTileEntity.class);
        if(te!=null)
            te.getNetwork().startConnectionFromBlock(te);
    }
}

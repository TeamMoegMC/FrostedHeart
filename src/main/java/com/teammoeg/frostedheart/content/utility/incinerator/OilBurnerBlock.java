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

package com.teammoeg.frostedheart.content.utility.incinerator;

import java.util.Random;
import java.util.function.Supplier;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.base.block.FHEntityBlock;
import com.teammoeg.frostedheart.util.FHUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidUtil;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class OilBurnerBlock extends FHBaseBlock implements FHEntityBlock<OilBurnerTileEntity>{

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public OilBurnerBlock(Properties blockProps) {
        super(blockProps.lightLevel(FHUtils.getLightValueLit(15)));
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, Boolean.FALSE));
    }

    @Override
    public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, RandomSource rand) {
        super.animateTick(stateIn, worldIn, pos, rand);
        if (stateIn.getValue(LIT)) {
            for (int i = 0; i < rand.nextInt(2) + 2; ++i) {
                ClientUtils.spawnSmokeParticles(worldIn, pos.above());
                ClientUtils.spawnFireParticles(worldIn, pos.above());
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player,
                                             InteractionHand handIn, BlockHitResult hit) {
        if (FluidUtil.interactWithFluidHandler(player, handIn, worldIn, pos, hit.getDirection()))
            return InteractionResult.SUCCESS;
        return super.use(state, worldIn, pos, player, handIn, hit);
    }

    @Override
    public void stepOn(Level w, BlockPos p,BlockState bs, Entity e) {
        if (bs.getValue(LIT))
            if (e instanceof LivingEntity)
                e.setSecondsOnFire(60);
    }

	@Override
	public Supplier<BlockEntityType<OilBurnerTileEntity>> getBlock() {
		return FHTileTypes.OIL_BURNER;
	}

}

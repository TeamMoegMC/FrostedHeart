package com.teammoeg.frostedheart.content.steamenergy.sauna;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.client.util.ClientUtils;
import com.teammoeg.frostedheart.content.steamenergy.ISteamEnergyBlock;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;
import java.util.function.BiFunction;

public class SaunaBlock extends FHBaseBlock implements ISteamEnergyBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public SaunaBlock(String name, Properties blockProps,
                        BiFunction<Block, Item.Properties, Item> createItemBlock) {
        super(name, blockProps, createItemBlock);
        this.setDefaultState(this.stateContainer.getBaseState().with(LIT, Boolean.FALSE).with(BlockStateProperties.FACING, Direction.SOUTH));
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.SAUNA.get().create();
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(BlockStateProperties.FACING);
        builder.add(LIT);
    }

    @Override
    public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        super.animateTick(stateIn, worldIn, pos, rand);
        if (stateIn.get(LIT)) {
            ClientUtils.spawnSteamParticles(worldIn, pos);
        }
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        ActionResultType superResult = super.onBlockActivated(state, world, pos, player, hand, hit);
        if (superResult.isSuccessOrConsume() || player.isSneaking())
            return superResult;
        TileEntity te = Utils.getExistingTileEntity(world, pos);
        if (te instanceof SaunaTileEntity) {
            return ((SaunaTileEntity) te).onClick(player);
        }
        return superResult;
    }
}

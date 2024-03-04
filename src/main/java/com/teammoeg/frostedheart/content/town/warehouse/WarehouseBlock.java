package com.teammoeg.frostedheart.content.town.warehouse;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import com.teammoeg.frostedheart.content.climate.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
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
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Random;

public class WarehouseBlock extends FHBaseBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public WarehouseBlock(Properties blockProps) {
        super(blockProps);
        this.setDefaultState(this.stateContainer.getBaseState().with(LIT, Boolean.FALSE).with(BlockStateProperties.FACING, Direction.SOUTH));
    }
    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(LIT,BlockStateProperties.FACING);
    }
    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.WAREHOUSE.get().create();
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        super.animateTick(stateIn, worldIn, pos, rand);
        if (stateIn.get(LIT)) {
            ClientUtils.spawnSteamParticles(worldIn, pos);
        }
    }

    //test
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isRemote && handIn == Hand.MAIN_HAND) {
            WarehouseTileEntity te = (WarehouseTileEntity) worldIn.getTileEntity(pos);
            if (te == null) {
                return ActionResultType.FAIL;
            }
            player.sendStatusMessage(new StringTextComponent(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.sendStatusMessage(new StringTextComponent(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.sendStatusMessage(new StringTextComponent("Volume: " + (te.volume)), false);
            player.sendStatusMessage(new StringTextComponent("Area: " + (te.area)), false);
            player.sendStatusMessage(new StringTextComponent("Capacity: " + BigDecimal.valueOf(te.capacity)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()), false);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, entity, stack);
        WarehouseTileEntity te = (WarehouseTileEntity) Utils.getExistingTileEntity(world, pos);
        if (te != null) {
            // register the warehouse to the town
            if (entity instanceof ServerPlayerEntity) {
                if (ChunkHeatData.hasAdjust(world, pos)) {
                    TeamTown.from((PlayerEntity) entity).addTownBlock(pos, te);
                }
            }
        }
    }

}

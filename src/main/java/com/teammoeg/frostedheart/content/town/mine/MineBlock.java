package com.teammoeg.frostedheart.content.town.mine;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.town.*;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

//矿场方块，不是我的方块
public class MineBlock extends FHTownBuildingCoreBlock {

    public MineBlock(Properties blockProps){
        super(blockProps);
    }

    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.MINE.get().create();
    }

    //test
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isRemote && handIn == Hand.MAIN_HAND) {
            MineTileEntity te = (MineTileEntity) worldIn.getTileEntity(pos);
            if (te == null) {
                return ActionResultType.FAIL;
            }
            te.refresh();
            player.sendStatusMessage(new StringTextComponent(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.sendStatusMessage(new StringTextComponent(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.sendStatusMessage(new StringTextComponent("Valid stone: " + (te.getValidStoneOrOre())), false);
            player.sendStatusMessage(new StringTextComponent("Average light level: " + (te.getAvgLightLevel())), false);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, entity, stack);
        MineTileEntity te = (MineTileEntity) Utils.getExistingTileEntity(world, pos);
        if (te != null) {
            // register the house to the town
            if (entity instanceof ServerPlayerEntity) {
                if (ChunkHeatData.hasAdjust(world, pos)) {
                    TeamTown.from((PlayerEntity) entity).addTownBlock(pos, te);
                    TeamTown.from((PlayerEntity) entity).getTownBlocks().values().stream()
                            .filter((townWorkerData)->townWorkerData.getType() == TownWorkerType.MINE_BASE)
                            .map(TownWorkerData::getWorkData)
                            .map((workData)->workData.getDouble("rating"))
                            .forEach(te::setLinkedBaseRating);
                }
            }
        }
    }
}

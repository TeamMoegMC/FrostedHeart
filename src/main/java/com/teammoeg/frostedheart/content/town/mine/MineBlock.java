package com.teammoeg.frostedheart.content.town.mine;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.town.*;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.LongNBT;
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
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

//矿场方块，不是我的方块
import net.minecraft.block.AbstractBlock.Properties;

public class MineBlock extends AbstractTownWorkerBlock {

    public MineBlock(Properties blockProps){
        super(blockProps);
    }

    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.MINE.get().create();
    }

    //test
    public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isClientSide && handIn == Hand.MAIN_HAND) {
            MineTileEntity te = (MineTileEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return ActionResultType.FAIL;
            }
            te.refresh();
            player.displayClientMessage(new StringTextComponent(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.displayClientMessage(new StringTextComponent(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.displayClientMessage(new StringTextComponent("Valid stone: " + (te.getValidStoneOrOre())), false);
            player.displayClientMessage(new StringTextComponent("Average light level: " + (te.getAvgLightLevel())), false);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }

    @Override
    public void setPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(world, pos, state, entity, stack);
        MineTileEntity te = (MineTileEntity) Utils.getExistingTileEntity(world, pos);
        if (te != null) {
            if (entity instanceof ServerPlayerEntity) {
                //if (ChunkHeatData.hasAdjust(world, pos)) { 矿坑的工作不强制要求能量塔在附近
                TeamTown.from((PlayerEntity) entity).addTownBlock(pos, te);

                //让所在Town的所有MineBase都检查一下连接的矿坑，并且更新矿坑的linkedBaseRating
                TeamTown.from((PlayerEntity)entity).getTownBlocks().values()
                        .stream()
                        .filter(data -> data.getType() == TownWorkerType.MINE_BASE
                                && data.getWorkData().getByte("isValid") == 1)
                        .map(data -> (MineBaseTileEntity) Utils.getExistingTileEntity(world, data.getPos()))
                        .filter(mineBaseTileEntity -> {
                            mineBaseTileEntity.refresh();
                            return mineBaseTileEntity.getLinkedMines().contains(pos);
                        })
                        .forEach(mineBaseTileEntity -> te.setLinkedBase(mineBaseTileEntity.getBlockPos(), mineBaseTileEntity.getRating()));
            }
        }
    }
}

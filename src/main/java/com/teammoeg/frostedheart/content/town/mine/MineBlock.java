package com.teammoeg.frostedheart.content.town.mine;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.town.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.LongNBT;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

//矿场方块，不是我的方块
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class MineBlock extends AbstractTownWorkerBlock {

    public MineBlock(Properties blockProps){
        super(blockProps);
    }

    @Override
    public BlockEntity createTileEntity(@Nonnull BlockState state, @Nonnull BlockGetter world) {
        return FHTileTypes.MINE.get().create();
    }

    //test
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND) {
            MineTileEntity te = (MineTileEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            te.refresh();
            player.displayClientMessage(new TextComponent(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.displayClientMessage(new TextComponent(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.displayClientMessage(new TextComponent("Valid stone: " + (te.getValidStoneOrOre())), false);
            player.displayClientMessage(new TextComponent("Average light level: " + (te.getAvgLightLevel())), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(world, pos, state, entity, stack);
        MineTileEntity te = (MineTileEntity) Utils.getExistingTileEntity(world, pos);
        if (te != null) {
            if (entity instanceof ServerPlayer) {
                //if (ChunkHeatData.hasAdjust(world, pos)) { 矿坑的工作不强制要求能量塔在附近
                TeamTown.from((Player) entity).addTownBlock(pos, te);
            }
        }
    }
}

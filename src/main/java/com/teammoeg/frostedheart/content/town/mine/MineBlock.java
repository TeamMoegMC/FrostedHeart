package com.teammoeg.frostedheart.content.town.mine;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.FHBlockEntityTypes;
import com.teammoeg.frostedheart.base.block.FHEntityBlock;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlock;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.util.Lang;

import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MineBlock extends AbstractTownWorkerBlock implements FHEntityBlock<MineBlockEntity>{

    public MineBlock(Properties blockProps){
        super(blockProps);
    }



    //test
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND) {
            MineBlockEntity te = (MineBlockEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            te.refresh();
            player.displayClientMessage(Lang.str(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.displayClientMessage(Lang.str(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.displayClientMessage(Lang.str("Valid stone: " + (te.getValidStoneOrOre())), false);
            player.displayClientMessage(Lang.str("Average light level: " + (te.getAvgLightLevel())), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(world, pos, state, entity, stack);
        MineBlockEntity te = (MineBlockEntity) Utils.getExistingTileEntity(world, pos);
        if (te != null) {
            if (entity instanceof ServerPlayer) {
                //if (ChunkHeatData.hasAdjust(world, pos)) { 矿坑的工作不强制要求能量塔在附近
                TeamTown.from((Player) entity).addTownBlock(pos, te);
            }
        }
    }



	@Override
	public Supplier<BlockEntityType<MineBlockEntity>> getBlock() {
		return FHBlockEntityTypes.MINE;
	}
}

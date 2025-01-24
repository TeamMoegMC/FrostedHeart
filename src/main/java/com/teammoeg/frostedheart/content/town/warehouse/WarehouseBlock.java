package com.teammoeg.frostedheart.content.town.warehouse;

import blusunrize.immersiveengineering.common.util.Utils;
import com.teammoeg.chorda.block.CEntityBlock;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.chorda.team.CTeamDataManager;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlock;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import java.util.function.Supplier;

public class WarehouseBlock extends AbstractTownWorkerBlock implements CEntityBlock<WarehouseBlockEntity> {
    public WarehouseBlock(Properties blockProps) {
        super(blockProps);
    }


    //test
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND) {
            WarehouseBlockEntity te = (WarehouseBlockEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            player.displayClientMessage(Components.str(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.displayClientMessage(Components.str(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.displayClientMessage(Components.str("Volume: " + (te.getVolume())), false);
            player.displayClientMessage(Components.str("Area: " + (te.getArea())), false);
            player.displayClientMessage(Components.str("Capacity: " + BigDecimal.valueOf(te.getCapacity())
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

	@Override
	public Supplier<BlockEntityType<WarehouseBlockEntity>> getBlock() {
		return FHBlockEntityTypes.WAREHOUSE;
	}

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(world, pos, state, entity, stack);
        WarehouseBlockEntity warehouseBlockEntity = (WarehouseBlockEntity) Utils.getExistingTileEntity(world, pos);
        if (warehouseBlockEntity != null) {
            if (entity instanceof ServerPlayer) {
                if (ChunkHeatData.hasAdjust(world, pos)) {
                    UUID teamFHID = CTeamDataManager.get((ServerPlayer)entity).getId();
                    warehouseBlockEntity.setTeamID(teamFHID);
                }
            }
        }
    }
}

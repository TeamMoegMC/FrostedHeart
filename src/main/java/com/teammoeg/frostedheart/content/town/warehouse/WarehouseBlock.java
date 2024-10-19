package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHEntityBlock;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlock;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Supplier;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class WarehouseBlock extends AbstractTownWorkerBlock implements FHEntityBlock<WarehouseTileEntity>{
    public WarehouseBlock(Properties blockProps) {
        super(blockProps);
    }


    //test
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND) {
            WarehouseTileEntity te = (WarehouseTileEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            player.displayClientMessage(TranslateUtils.str(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.displayClientMessage(TranslateUtils.str(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.displayClientMessage(TranslateUtils.str("Volume: " + (te.getVolume())), false);
            player.displayClientMessage(TranslateUtils.str("Area: " + (te.getArea())), false);
            player.displayClientMessage(TranslateUtils.str("Capacity: " + BigDecimal.valueOf(te.getCapacity())
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

	@Override
	public Supplier<BlockEntityType<WarehouseTileEntity>> getBlock() {

		return FHTileTypes.WAREHOUSE;
	}
}

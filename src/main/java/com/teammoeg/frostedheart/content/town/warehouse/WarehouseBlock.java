package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.frostedheart.base.block.FHEntityBlock;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlock;
import com.teammoeg.frostedheart.util.lang.Lang;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Supplier;

public class WarehouseBlock extends AbstractTownWorkerBlock implements FHEntityBlock<WarehouseBlockEntity>{
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
            player.displayClientMessage(Lang.str(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.displayClientMessage(Lang.str(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.displayClientMessage(Lang.str("Volume: " + (te.getVolume())), false);
            player.displayClientMessage(Lang.str("Area: " + (te.getArea())), false);
            player.displayClientMessage(Lang.str("Capacity: " + BigDecimal.valueOf(te.getCapacity())
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

	@Override
	public Supplier<BlockEntityType<WarehouseBlockEntity>> getBlock() {

		return FHBlockEntityTypes.WAREHOUSE;
	}
}

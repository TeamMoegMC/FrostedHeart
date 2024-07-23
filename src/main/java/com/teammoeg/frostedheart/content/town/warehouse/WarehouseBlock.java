package com.teammoeg.frostedheart.content.town.warehouse;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.RoundingMode;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class WarehouseBlock extends AbstractTownWorkerBlock {
    public WarehouseBlock(Properties blockProps) {
        super(blockProps);
    }

    @Override
    public BlockEntity createTileEntity(@Nonnull BlockState state, @Nonnull BlockGetter world) {
        return FHTileTypes.WAREHOUSE.get().create();
    }

    //test
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND) {
            WarehouseTileEntity te = (WarehouseTileEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            player.displayClientMessage(new TextComponent(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.displayClientMessage(new TextComponent(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.displayClientMessage(new TextComponent("Volume: " + (te.getVolume())), false);
            player.displayClientMessage(new TextComponent("Area: " + (te.getArea())), false);
            player.displayClientMessage(new TextComponent("Capacity: " + BigDecimal.valueOf(te.getCapacity())
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}

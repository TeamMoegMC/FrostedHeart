package com.teammoeg.frostedheart.content.town.mine;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.base.block.FHEntityBlock;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlock;
import com.teammoeg.frostedheart.util.lang.Lang;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class MineBaseBlock extends AbstractTownWorkerBlock implements FHEntityBlock<MineBaseBlockEntity>{

    public MineBaseBlock(Properties blockProps) {
        super(blockProps);
    }


    //test
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND) {
            MineBaseBlockEntity te = (MineBaseBlockEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            player.displayClientMessage(Lang.str(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.displayClientMessage(Lang.str(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.displayClientMessage(Lang.str("Area: " + (te.getArea())), false);
            player.displayClientMessage(Lang.str("Volume: " + (te.getVolume())), false);
            player.displayClientMessage(Lang.str("Chest: " + (te.getChest())), false);
            player.displayClientMessage(Lang.str("Rack: " + (te.getRack())), false);
            player.displayClientMessage(Lang.str("Linked mines: " + (te.linkedMines)), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }


	@Override
	public Supplier<BlockEntityType<MineBaseBlockEntity>> getBlock() {
		return FHBlockEntityTypes.MINE_BASE;
	}
}

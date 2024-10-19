package com.teammoeg.frostedheart.content.town.hunting;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.base.block.FHEntityBlock;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlock;
import com.teammoeg.frostedheart.util.TranslateUtils;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;

public class HuntingCampBlock extends AbstractTownWorkerBlock implements FHEntityBlock<HuntingCampTileEntity>{
    public HuntingCampBlock(Properties blockProps) {
        super(blockProps);
    }



    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND) {
            HuntingCampTileEntity te = (HuntingCampTileEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            player.displayClientMessage(TranslateUtils.str(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }



	@Override
	public Supplier<BlockEntityType<HuntingCampTileEntity>> getBlock() {
		return FHTileTypes.HUNTING_CAMP;
	}
}

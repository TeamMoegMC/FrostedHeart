package com.teammoeg.frostedheart.content.town.hunting;

import java.util.function.Supplier;

import com.teammoeg.frostedheart.FHBlockEntityTypes;
import com.teammoeg.frostedheart.base.block.FHEntityBlock;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlock;
import com.teammoeg.frostedheart.util.Lang;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;

public class HuntingCampBlock extends AbstractTownWorkerBlock implements FHEntityBlock<HuntingCampBlockEntity>{
    public HuntingCampBlock(Properties blockProps) {
        super(blockProps);
    }



    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND) {
            HuntingCampBlockEntity te = (HuntingCampBlockEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            player.displayClientMessage(Lang.str(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }



	@Override
	public Supplier<BlockEntityType<HuntingCampBlockEntity>> getBlock() {
		return FHBlockEntityTypes.HUNTING_CAMP;
	}
}

package com.teammoeg.frostedheart.content.town.hunting;

import com.teammoeg.chorda.block.FHEntityBlock;
import com.teammoeg.chorda.util.lang.Components;
import com.teammoeg.frostedheart.bootstrap.common.FHBlockEntityTypes;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlock;
import com.teammoeg.frostedheart.util.client.FHClientUtils;
import com.teammoeg.chorda.util.MathUtils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public class HuntingBaseBlock extends AbstractTownWorkerBlock implements FHEntityBlock<HuntingBaseBlockEntity>{
    public HuntingBaseBlock(Properties blockProps) {
        super(blockProps);
    }


    @Override
    public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, RandomSource rand) {
        super.animateTick(stateIn, worldIn, pos, rand);
        if (stateIn.getValue(AbstractTownWorkerBlock.LIT)) {
            FHClientUtils.spawnSteamParticles(worldIn, pos);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND) {
            HuntingBaseBlockEntity te = (HuntingBaseBlockEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            player.displayClientMessage(Components.str(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.displayClientMessage(Components.str(te.isTemperatureValid() ? "Valid temperature" : "Invalid temperature"), false);
            player.displayClientMessage(Components.str(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.displayClientMessage(Components.str("Raw temperature: " +
                    MathUtils.round(te.getTemperature(), 2)), false);
            player.displayClientMessage(Components.str("Temperature modifier: " +
                    MathUtils.round(te.getTemperatureModifier(), 2)), false);
            player.displayClientMessage(Components.str("Effective temperature: " +
                    MathUtils.round(te.getEffectiveTemperature(), 2)), false);
            player.displayClientMessage(Components.str("BedNum: " + te.getBedNum()), false);
            player.displayClientMessage(Components.str("MaxResident: " + te.getMaxResident()), false);
            player.displayClientMessage(Components.str("TanningRackNum: " + te.getTanningRackNum()), false);
            player.displayClientMessage(Components.str("chestNum: " + te.getChestNum()), false);
            player.displayClientMessage(Components.str("Volume: " + (te.getVolume())), false);
            player.displayClientMessage(Components.str("Area: " + (te.getArea())), false);
            player.displayClientMessage(Components.str("Rating: " +
                    MathUtils.round(te.getRating(), 2)), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }


	@Override
	public Supplier<BlockEntityType<HuntingBaseBlockEntity>> getBlock() {
		return FHBlockEntityTypes.HUNTING_BASE;
	}
}

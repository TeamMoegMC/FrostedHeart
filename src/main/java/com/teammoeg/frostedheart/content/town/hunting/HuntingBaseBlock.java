package com.teammoeg.frostedheart.content.town.hunting;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlock;
import com.teammoeg.frostedheart.content.town.house.HouseTileEntity;
import com.teammoeg.frostedheart.content.town.mine.MineBaseTileEntity;
import com.teammoeg.frostedheart.util.MathUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;
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
import java.util.Random;

import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class HuntingBaseBlock extends AbstractTownWorkerBlock {
    public HuntingBaseBlock(Properties blockProps) {
        super(blockProps);
    }

    @Override
    public BlockEntity createTileEntity(@Nonnull BlockState state, @Nonnull BlockGetter world) {
        return FHTileTypes.HUNTING_BASE.get().create();
    }

    @Override
    public void animateTick(BlockState stateIn, Level worldIn, BlockPos pos, Random rand) {
        super.animateTick(stateIn, worldIn, pos, rand);
        if (stateIn.getValue(AbstractTownWorkerBlock.LIT)) {
            ClientUtils.spawnSteamParticles(worldIn, pos);
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND) {
            HuntingBaseTileEntity te = (HuntingBaseTileEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            player.displayClientMessage(new TextComponent(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.displayClientMessage(new TextComponent(te.isTemperatureValid() ? "Valid temperature" : "Invalid temperature"), false);
            player.displayClientMessage(new TextComponent(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.displayClientMessage(new TextComponent("Raw temperature: " +
                    MathUtils.round(te.getTemperature(), 2)), false);
            player.displayClientMessage(new TextComponent("Temperature modifier: " +
                    MathUtils.round(te.getTemperatureModifier(), 2)), false);
            player.displayClientMessage(new TextComponent("Effective temperature: " +
                    MathUtils.round(te.getEffectiveTemperature(), 2)), false);
            player.displayClientMessage(new TextComponent("BedNum: " + te.getBedNum()), false);
            player.displayClientMessage(new TextComponent("MaxResident: " + te.getMaxResident()), false);
            player.displayClientMessage(new TextComponent("TanningRackNum: " + te.getTanningRackNum()), false);
            player.displayClientMessage(new TextComponent("chestNum: " + te.getChestNum()), false);
            player.displayClientMessage(new TextComponent("Volume: " + (te.getVolume())), false);
            player.displayClientMessage(new TextComponent("Area: " + (te.getArea())), false);
            player.displayClientMessage(new TextComponent("Rating: " +
                    MathUtils.round(te.getRating(), 2)), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}

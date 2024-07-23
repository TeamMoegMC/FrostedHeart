package com.teammoeg.frostedheart.content.town.hunting;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlock;
import com.teammoeg.frostedheart.content.town.house.HouseTileEntity;
import com.teammoeg.frostedheart.content.town.mine.MineBaseTileEntity;
import com.teammoeg.frostedheart.util.MathUtils;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Random;

import net.minecraft.block.AbstractBlock.Properties;

public class HuntingBaseBlock extends AbstractTownWorkerBlock {
    public HuntingBaseBlock(Properties blockProps) {
        super(blockProps);
    }

    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.HUNTING_BASE.get().create();
    }

    @Override
    public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        super.animateTick(stateIn, worldIn, pos, rand);
        if (stateIn.getValue(AbstractTownWorkerBlock.LIT)) {
            ClientUtils.spawnSteamParticles(worldIn, pos);
        }
    }

    @Override
    public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isClientSide && handIn == Hand.MAIN_HAND) {
            HuntingBaseTileEntity te = (HuntingBaseTileEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return ActionResultType.FAIL;
            }
            player.displayClientMessage(new StringTextComponent(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.displayClientMessage(new StringTextComponent(te.isTemperatureValid() ? "Valid temperature" : "Invalid temperature"), false);
            player.displayClientMessage(new StringTextComponent(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.displayClientMessage(new StringTextComponent("Raw temperature: " +
                    MathUtils.round(te.getTemperature(), 2)), false);
            player.displayClientMessage(new StringTextComponent("Temperature modifier: " +
                    MathUtils.round(te.getTemperatureModifier(), 2)), false);
            player.displayClientMessage(new StringTextComponent("Effective temperature: " +
                    MathUtils.round(te.getEffectiveTemperature(), 2)), false);
            player.displayClientMessage(new StringTextComponent("BedNum: " + te.getBedNum()), false);
            player.displayClientMessage(new StringTextComponent("MaxResident: " + te.getMaxResident()), false);
            player.displayClientMessage(new StringTextComponent("TanningRackNum: " + te.getTanningRackNum()), false);
            player.displayClientMessage(new StringTextComponent("chestNum: " + te.getChestNum()), false);
            player.displayClientMessage(new StringTextComponent("Volume: " + (te.getVolume())), false);
            player.displayClientMessage(new StringTextComponent("Area: " + (te.getArea())), false);
            player.displayClientMessage(new StringTextComponent("Rating: " +
                    MathUtils.round(te.getRating(), 2)), false);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }
}

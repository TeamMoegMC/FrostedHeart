package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.town.FHTownBuildingCoreBlock;
import com.teammoeg.frostedheart.content.town.warehouse.WarehouseTileEntity;
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
import java.math.BigDecimal;
import java.math.RoundingMode;

//矿场方块，不是我的方块
public class MineBlock extends FHTownBuildingCoreBlock {

    public MineBlock(Properties blockProps){
        super(blockProps);
    }

    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.MINE.get().create();
    }

    //test
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isRemote && handIn == Hand.MAIN_HAND) {
            MineTileEntity te = (MineTileEntity) worldIn.getTileEntity(pos);
            if (te == null) {
                return ActionResultType.FAIL;
            }
            player.sendStatusMessage(new StringTextComponent(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.sendStatusMessage(new StringTextComponent(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.sendStatusMessage(new StringTextComponent("Valid stone: " + (te.validStoneOrOre)), false);
            player.sendStatusMessage(new StringTextComponent("Average light level: " + (te.avgLightLevel)), false);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }

}

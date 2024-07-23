package com.teammoeg.frostedheart.content.town.mine;

import com.teammoeg.frostedheart.FHTileTypes;
import com.teammoeg.frostedheart.content.town.AbstractTownWorkerBlock;
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
//矿场基地方块，不是矿场的BaseBlock
import net.minecraft.block.AbstractBlock.Properties;

public class MineBaseBlock extends AbstractTownWorkerBlock {

    public MineBaseBlock(Properties blockProps) {
        super(blockProps);
    }

    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return FHTileTypes.MINE_BASE.get().create();
    }

    //test
    public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isClientSide && handIn == Hand.MAIN_HAND) {
            MineBaseTileEntity te = (MineBaseTileEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return ActionResultType.FAIL;
            }
            player.displayClientMessage(new StringTextComponent(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.displayClientMessage(new StringTextComponent(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.displayClientMessage(new StringTextComponent("Area: " + (te.getArea())), false);
            player.displayClientMessage(new StringTextComponent("Volume: " + (te.getVolume())), false);
            player.displayClientMessage(new StringTextComponent("Chest: " + (te.getChest())), false);
            player.displayClientMessage(new StringTextComponent("Rack: " + (te.getRack())), false);
            player.displayClientMessage(new StringTextComponent("Linked mines: " + (te.linkedMines)), false);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }
}

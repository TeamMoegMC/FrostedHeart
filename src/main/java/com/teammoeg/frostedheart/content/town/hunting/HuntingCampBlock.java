package com.teammoeg.frostedheart.content.town.hunting;

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

import net.minecraft.block.AbstractBlock.Properties;

public class HuntingCampBlock extends AbstractTownWorkerBlock {
    public HuntingCampBlock(Properties blockProps) {
        super(blockProps);
    }

    @Override
    public TileEntity createTileEntity(@Nonnull BlockState state, @Nonnull IBlockReader world) {
        return null;
    }

    @Override
    public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        if (!worldIn.isClientSide && handIn == Hand.MAIN_HAND) {
            HuntingCampTileEntity te = (HuntingCampTileEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return ActionResultType.FAIL;
            }
            player.displayClientMessage(new StringTextComponent(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }
}

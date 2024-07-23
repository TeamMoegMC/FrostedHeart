package com.teammoeg.frostedheart.content.town.mine;

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
//矿场基地方块，不是矿场的BaseBlock
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

public class MineBaseBlock extends AbstractTownWorkerBlock {

    public MineBaseBlock(Properties blockProps) {
        super(blockProps);
    }

    @Override
    public BlockEntity createTileEntity(@Nonnull BlockState state, @Nonnull BlockGetter world) {
        return FHTileTypes.MINE_BASE.get().create();
    }

    //test
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (!worldIn.isClientSide && handIn == InteractionHand.MAIN_HAND) {
            MineBaseTileEntity te = (MineBaseTileEntity) worldIn.getBlockEntity(pos);
            if (te == null) {
                return InteractionResult.FAIL;
            }
            player.displayClientMessage(new TextComponent(te.isWorkValid() ? "Valid working environment" : "Invalid working environment"), false);
            player.displayClientMessage(new TextComponent(te.isStructureValid() ? "Valid structure" : "Invalid structure"), false);
            player.displayClientMessage(new TextComponent("Area: " + (te.getArea())), false);
            player.displayClientMessage(new TextComponent("Volume: " + (te.getVolume())), false);
            player.displayClientMessage(new TextComponent("Chest: " + (te.getChest())), false);
            player.displayClientMessage(new TextComponent("Rack: " + (te.getRack())), false);
            player.displayClientMessage(new TextComponent("Linked mines: " + (te.linkedMines)), false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}

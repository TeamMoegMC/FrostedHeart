package com.teammoeg.frostedheart.content.decoration;

import com.cannolicatfish.rankine.blocks.RankineOreBlock;
import com.cannolicatfish.rankine.util.WorldgenUtils;
import com.teammoeg.frostedheart.FHContent;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.block.FHBaseBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.state.Property;
import net.minecraft.state.StateContainer;
import net.minecraft.world.World;

import java.util.function.BiFunction;

public class FHOreBlock extends FHBaseBlock {
    public FHOreBlock(String name, Properties blockProps, BiFunction<Block, Item.Properties, Item> createItemBlock) {
        super(name, blockProps, createItemBlock);
    }

    public BlockState getStateForPlacement(BlockItemUseContext context) {
        World world = context.getWorld();
        BlockState target = world.getBlockState(context.getPos().offset(context.getFace().getOpposite()));
        if (target.getBlock() instanceof RankineOreBlock) {
            return this.getDefaultState().with(RankineOreBlock.TYPE, target.get(RankineOreBlock.TYPE));
        } else {
            return WorldgenUtils.ORE_STONES.contains(target.getBlock()) ? this.getDefaultState().with(RankineOreBlock.TYPE, WorldgenUtils.ORE_STONES.indexOf(target.getBlock())) : this.getDefaultState().with(RankineOreBlock.TYPE, 0);
        }
    }

    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(RankineOreBlock.TYPE);
    }
}

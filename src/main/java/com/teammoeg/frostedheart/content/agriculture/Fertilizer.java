package com.teammoeg.frostedheart.content.agriculture;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.item.FHBaseItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class Fertilizer extends FHBaseItem {
    private final FertilizerType type;
    public Fertilizer(Properties properties,FertilizerType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);
        ItemStack itemstack = pContext.getItemInHand();

        if(blockstate.is(Blocks.FARMLAND)){
            level.setBlock(blockpos,FHBlocks.FERTILIZED_FARMLAND.getDefaultState().setValue(FertilizedFarmlandBlock.FERTILIZER,this.type.getType()), 2);
            return InteractionResult.SUCCESS;
        }
        if(blockstate.is(Blocks.DIRT)|| blockstate.is(Blocks.GRASS_BLOCK)){
            level.setBlock(blockpos,FHBlocks.FERTILIZED_DIRT.getDefaultState().setValue(FertilizedDirt.FERTILIZER,this.type.getType()), 2);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public FertilizerType getType() {
        return type;
    }

    public enum FertilizerType {
        INCREASING_FERTILIZER(1),
        ACCELERATED_FERTILIZER(2),
        PRESERVED_FERTILIZER(3);

        private final int type;
        FertilizerType(int type) {
            this.type = type;
        }
        public int getType() {
            return type;
        }
    }
}

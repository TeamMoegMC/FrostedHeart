package com.teammoeg.frostedheart.content.agriculture;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.item.FHBaseItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public class Fertilizer extends FHBaseItem {
    private final FertilizerGrade grade;
    private final FertilizerType type;
    public Fertilizer(Properties properties,FertilizerType type,FertilizerGrade grade) {
        super(properties);
        this.type = type;
        this.grade = grade;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltipComponents, flag);

        FertilizerGrade grade = this.getGrade();
        if (grade == FertilizerGrade.BASIC) {
            tooltipComponents.add(Component.translatable("tooltip.frostedheart.fertilizer.basic").withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.frostedheart.fertilizer.advanced").withStyle(ChatFormatting.GOLD));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);
        ItemStack itemstack = pContext.getItemInHand();

        if(blockstate.is(Blocks.FARMLAND)){
            level.setBlock(blockpos, FHBlocks.FERTILIZED_FARMLAND.getDefaultState().setValue(FertilizedFarmlandBlock.FERTILIZER,this.type.getType()).setValue(FertilizedFarmlandBlock.ADVANCED,getGrade()==FertilizerGrade.ADVANCED).setValue(FertilizedFarmlandBlock.STORAGE, 15), 2);
            return InteractionResult.SUCCESS;
        }
        if(blockstate.is(FHBlocks.FERTILIZED_FARMLAND.get())){
            int currentStorage = blockstate.getValue(FertilizedFarmlandBlock.STORAGE);
            int newStorage = Math.min(currentStorage + 15, 30);
            level.setBlock(blockpos, blockstate.setValue(FertilizedFarmlandBlock.STORAGE, newStorage).setValue(FertilizedFarmlandBlock.FERTILIZER,this.type.getType()).setValue(FertilizedFarmlandBlock.ADVANCED,getGrade()==FertilizerGrade.ADVANCED), 2);
            return InteractionResult.SUCCESS;
        }

        if(blockstate.is(Blocks.DIRT)|| blockstate.is(Blocks.GRASS_BLOCK)){
            level.setBlock(blockpos,FHBlocks.FERTILIZED_DIRT.getDefaultState().setValue(FertilizedDirt.FERTILIZER,this.type.getType()).setValue(FertilizedDirt.ADVANCED,getGrade()==FertilizerGrade.ADVANCED).setValue(FertilizedDirt.STORAGE, 15), 2);
            return InteractionResult.SUCCESS;
        }
        if(blockstate.is(FHBlocks.FERTILIZED_DIRT.get())){
            int currentStorage = blockstate.getValue(FertilizedDirt.STORAGE);
            int newStorage = Math.min(currentStorage + 15, 30);
            level.setBlock(blockpos, blockstate.setValue(FertilizedDirt.STORAGE, newStorage).setValue(FertilizedDirt.FERTILIZER,this.type.getType()).setValue(FertilizedDirt.ADVANCED,getGrade()==FertilizerGrade.ADVANCED), 2);
            return InteractionResult.SUCCESS;
        }
        if(blockstate.getTags().anyMatch(t->t==BlockTags.CROPS)){
            BlockState blockstate1 = level.getBlockState(blockpos.below());
            if(blockstate1.is(Blocks.FARMLAND)){
                level.setBlock(blockpos.below(),FHBlocks.FERTILIZED_FARMLAND.getDefaultState().setValue(FertilizedFarmlandBlock.MOISTURE,blockstate1.getValue(FarmBlock.MOISTURE)).setValue(FertilizedFarmlandBlock.FERTILIZER,this.type.getType()).setValue(FertilizedFarmlandBlock.ADVANCED,getGrade()==FertilizerGrade.ADVANCED).setValue(FertilizedFarmlandBlock.STORAGE, 15), 2);
                return InteractionResult.SUCCESS;
            }
            if(blockstate1.is(FHBlocks.FERTILIZED_FARMLAND.get())){
                int currentStorage = blockstate1.getValue(FertilizedFarmlandBlock.STORAGE);
                int newStorage = Math.min(currentStorage + 15, 30);
                level.setBlock(blockpos.below(), blockstate1.setValue(FertilizedFarmlandBlock.STORAGE, newStorage).setValue(FertilizedFarmlandBlock.MOISTURE,blockstate1.getValue(FarmBlock.MOISTURE)).setValue(FertilizedFarmlandBlock.FERTILIZER,this.type.getType()).setValue(FertilizedFarmlandBlock.ADVANCED,getGrade()==FertilizerGrade.ADVANCED), 2);
                return InteractionResult.SUCCESS;
            }
        }
        if(blockstate.getTags().anyMatch(t->t==BlockTags.SAPLINGS)){
            BlockState blockstate1 = level.getBlockState(blockpos.below());
            if(blockstate1.is(Blocks.DIRT)|| blockstate1.is(Blocks.GRASS_BLOCK)){
                level.setBlock(blockpos.below(),FHBlocks.FERTILIZED_DIRT.getDefaultState().setValue(FertilizedDirt.FERTILIZER,this.type.getType()).setValue(FertilizedFarmlandBlock.ADVANCED,getGrade()==FertilizerGrade.ADVANCED).setValue(FertilizedDirt.STORAGE, 15), 2);
                return InteractionResult.SUCCESS;
            }
            if(blockstate1.is(FHBlocks.FERTILIZED_DIRT.get())){
                int currentStorage = blockstate1.getValue(FertilizedDirt.STORAGE);
                int newStorage = Math.min(currentStorage + 15, 30);
                level.setBlock(blockpos.below(), blockstate1.setValue(FertilizedDirt.STORAGE, newStorage).setValue(FertilizedDirt.FERTILIZER,this.type.getType()).setValue(FertilizedFarmlandBlock.ADVANCED,getGrade()==FertilizerGrade.ADVANCED), 2);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    public FertilizerType getType() {
        return type;
    }

    public FertilizerGrade getGrade() {
        return grade;
    }
    public enum FertilizerGrade {
        BASIC,
        ADVANCED
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

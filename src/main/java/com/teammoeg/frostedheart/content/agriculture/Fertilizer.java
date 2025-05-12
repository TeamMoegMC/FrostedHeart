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
    private static final String TAG_GRADE = "FertilizerGrade";
    private final FertilizerType type;
    public Fertilizer(Properties properties,FertilizerType type) {
        super(properties);
        this.type = type;
    }

    public static ItemStack setGrade(ItemStack stack, FertilizerGrade grade) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt(TAG_GRADE, grade.ordinal());
        return stack;
    }

    public static FertilizerGrade getGrade(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (tag.contains(TAG_GRADE)) {
            int ordinal = tag.getInt(TAG_GRADE);
            return FertilizerGrade.values()[ordinal];
        }
        return FertilizerGrade.BASIC;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltipComponents, flag);

        FertilizerGrade grade = getGrade(stack);
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
            level.setBlock(blockpos,FHBlocks.FERTILIZED_FARMLAND.getDefaultState().setValue(FertilizedFarmlandBlock.FERTILIZER,this.type.getType()).setValue(FertilizedFarmlandBlock.ADVANCED,getGrade(itemstack)==FertilizerGrade.ADVANCED), 2);
            return InteractionResult.SUCCESS;
        }
        if(blockstate.is(Blocks.DIRT)|| blockstate.is(Blocks.GRASS_BLOCK)){
            level.setBlock(blockpos,FHBlocks.FERTILIZED_DIRT.getDefaultState().setValue(FertilizedDirt.FERTILIZER,this.type.getType()).setValue(FertilizedDirt.ADVANCED,getGrade(itemstack)==FertilizerGrade.ADVANCED), 2);
            return InteractionResult.SUCCESS;
        }
        if(blockstate.getTags().anyMatch(t->t==BlockTags.CROPS)){
            BlockState blockstate1 = level.getBlockState(blockpos.below());
            if(blockstate1.is(Blocks.FARMLAND)){
                level.setBlock(blockpos.below(),FHBlocks.FERTILIZED_FARMLAND.getDefaultState().setValue(FertilizedFarmlandBlock.MOISTURE,blockstate1.getValue(FarmBlock.MOISTURE)).setValue(FertilizedFarmlandBlock.FERTILIZER,this.type.getType()).setValue(FertilizedFarmlandBlock.ADVANCED,getGrade(itemstack)==FertilizerGrade.ADVANCED), 2);
                return InteractionResult.SUCCESS;
            }
        }
        if(blockstate.getTags().anyMatch(t->t==BlockTags.SAPLINGS)){
            BlockState blockstate1 = level.getBlockState(blockpos.below());
            if(blockstate1.is(Blocks.DIRT)|| blockstate1.is(Blocks.GRASS_BLOCK)){
                level.setBlock(blockpos.below(),FHBlocks.FERTILIZED_DIRT.getDefaultState().setValue(FertilizedDirt.FERTILIZER,this.type.getType()).setValue(FertilizedFarmlandBlock.ADVANCED,getGrade(itemstack)==FertilizerGrade.ADVANCED), 2);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    public FertilizerType getType() {
        return type;
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

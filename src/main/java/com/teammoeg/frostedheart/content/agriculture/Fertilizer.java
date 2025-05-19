package com.teammoeg.frostedheart.content.agriculture;

import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.bootstrap.common.FHItems;
import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedheart.item.FHBaseItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
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
    public InteractionResult transform(Level level,BlockPos pos,BlockState blockstate) {
    	if(blockstate.is(Blocks.FARMLAND)){
    		level.setBlock(pos,FHBlocks.FERTILIZED_FARMLAND.getDefaultState()
            		.setValue(FertilizedDirt.FERTILIZER,this.type)
            		.setValue(FertilizedDirt.ADVANCED,getGrade())
            		.setValue(FertilizedDirt.STORAGE, 4),3);
            return InteractionResult.SUCCESS;
        }else if(blockstate.is(FHBlocks.FERTILIZED_FARMLAND.get())||blockstate.is(FHBlocks.FERTILIZED_DIRT.get())){
        	int currentStorage =0;
        	if(blockstate.getValue(FertilizedDirt.FERTILIZER)==this.type&&(blockstate.getValue(FertilizedDirt.ADVANCED)==getGrade())){
        		currentStorage=blockstate.getValue(FertilizedDirt.STORAGE);
        	}
            if(currentStorage<=4) {
            	currentStorage+=4;
            	level.setBlock(pos,blockstate.setValue(FertilizedDirt.STORAGE, currentStorage)
            			.setValue(FertilizedDirt.FERTILIZER,this.type)
                		.setValue(FertilizedDirt.ADVANCED,getGrade()),3);
            	return InteractionResult.SUCCESS;
            }
        }else if(blockstate.is(Blocks.DIRT)|| blockstate.is(Blocks.GRASS_BLOCK)){
    		level.setBlock(pos,FHBlocks.FERTILIZED_DIRT.getDefaultState()
            		.setValue(FertilizedDirt.FERTILIZER,this.type)
            		.setValue(FertilizedDirt.ADVANCED,getGrade())
            		.setValue(FertilizedDirt.STORAGE, 4),3);
            return InteractionResult.SUCCESS;
        }else if(blockstate.is(Blocks.COARSE_DIRT)){
        	if(getType()!=FertilizerType.PRESERVED) {
        		level.setBlock(pos,Blocks.DIRT.defaultBlockState(),3);
        		return InteractionResult.SUCCESS;
        	}
        }else if(blockstate.is(FHTags.Blocks.CROP.get())) {
        	BlockPos below=pos.below();
        	return transform(level,below,level.getBlockState(below));
        }
    	return InteractionResult.PASS;
    }
    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);
        return transform(level, blockpos, blockstate);
    }

    public FertilizerType getType() {
        return type;
    }

    public FertilizerGrade getGrade() {
        return grade;
    }
    
    public enum FertilizerGrade implements StringRepresentable {
        BASIC(1,1,2),
        ADVANCED(0.5f,2,3);
    	public final float growSpeed;
    	public final int preserve;
    	public final float productivity;
		@Override
		public String getSerializedName() {
			return this.name().toLowerCase();
		}
		private FertilizerGrade(float growSpeed, int preserve, float productivity) {
			this.growSpeed = growSpeed;
			this.preserve = preserve;
			this.productivity = productivity;
		}

    }

    public enum FertilizerType implements StringRepresentable{
        INCREASING(1),
        ACCELERATED(2),
        PRESERVED(3);

        private final int type;
        FertilizerType(int type) {
            this.type = type;
        }
        public int getType() {
            return type;
        }
		@Override
		public String getSerializedName() {
			return this.name().toLowerCase();
		}
    }
}

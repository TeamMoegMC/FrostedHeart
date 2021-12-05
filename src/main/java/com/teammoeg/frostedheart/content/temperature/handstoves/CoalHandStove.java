package com.teammoeg.frostedheart.content.temperature.handstoves;

import java.util.List;

import javax.annotation.Nullable;

import com.teammoeg.frostedheart.base.item.FHBaseItem;
import com.teammoeg.frostedheart.client.util.GuiUtils;
import com.teammoeg.frostedheart.climate.IHeatingEquipment;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.tags.ITag;
import net.minecraft.tags.TagCollectionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class CoalHandStove extends FHBaseItem implements IHeatingEquipment {
	public final static int max_fuel=800;
	public CoalHandStove(String name, Properties properties) {
		super(name, properties);
	}
	@Override
	public boolean isEnchantable(ItemStack stack) {
		return false;
	}

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
		list.add(GuiUtils.translateTooltip("handstove.add_fuel").mergeStyle(TextFormatting.GRAY));
		if(getAshAmount(stack)>=800)
		list.add(GuiUtils.translateTooltip("handstove.trash_ash").mergeStyle(TextFormatting.RED));
		list.add(GuiUtils.translateTooltip("handstove.fuel",getFuelAmount(stack)/2).mergeStyle(TextFormatting.GRAY));
    }

    @Override
    public float compute(ItemStack stack, float bodyTemp, float environmentTemp) {
		int fuel=getFuelAmount(stack);
		if(fuel>=2) {
			int ash=getAshAmount(stack);
			if(ash<=800) {
				fuel-=2;
				ash+=2;
				setFuelAmount(stack, fuel);
				setAshAmount(stack, ash);
		        if (bodyTemp < 0) {
		            return this.getMax(stack);
		        }
			}
		}
        return 0;
    }

    public static int getAshAmount(ItemStack is) {
    	return is.getOrCreateTag().getInt("ash");
    }
    public static int getFuelAmount(ItemStack is) {
    	return is.getOrCreateTag().getInt("fuel");
    }
    public static void setAshAmount(ItemStack is,int v) {
    	is.getOrCreateTag().putInt("ash",v);
    	if(v>=max_fuel)
    		is.getTag().putInt("CustomModelData", 2);
    }
    public static void setFuelAmount(ItemStack is,int v) {
    	is.getOrCreateTag().putInt("fuel",v);
    	if(v<2)
    		is.getTag().putInt("CustomModelData", 0);
    	else
    		is.getTag().putInt("CustomModelData", 1);
    }
    @Override
	public double getDurabilityForDisplay(ItemStack stack) {
		return getFuelAmount(stack)*1.0D/max_fuel;
	}
	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.EAT;
	}
	ResourceLocation ashitem=new ResourceLocation("frostedheart","ash");
	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
		ItemStack stack = playerIn.getHeldItem(handIn);
		ActionResult<ItemStack> FAIL = new ActionResult<>(ActionResultType.FAIL,stack);
		if(getAshAmount(playerIn.getHeldItem(handIn))>=800) {
			playerIn.setActiveHand(handIn);
			return new ActionResult<>(ActionResultType.SUCCESS, stack);
		}
		return FAIL; 
	}
	@Override
	public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
		int ash=getAshAmount(stack);
		if(ash>=800) {
			ITag<Item> item=TagCollectionManager.getManager().getItemTags().get(ashitem);
			setAshAmount(stack,ash-800);
	    	if(getFuelAmount(stack)<2)
	    		stack.getTag().putInt("CustomModelData", 0);
	    	else
	    		stack.getTag().putInt("CustomModelData", 1);
			if(item!=null&&entityLiving instanceof PlayerEntity&&!item.getAllElements().isEmpty()) {
				ItemStack ret=new ItemStack(item.getAllElements().get(0));
				if(!((PlayerEntity)entityLiving).addItemStackToInventory(ret)) 
					worldIn.addEntity(new ItemEntity(worldIn,entityLiving.getPosition().getX(),entityLiving.getPosition().getY(),entityLiving.getPosition().getZ(),ret));
			}
		}
		return stack;
	}



	@Override
	public int getUseDuration(ItemStack stack) {
		return 40;
	}
	@Override
    public float getMax(ItemStack stack) {
        return getFuelAmount(stack)>0?0.015F:0;
    }
	@Override
	public boolean canHandHeld() {
		return true;
	}
}

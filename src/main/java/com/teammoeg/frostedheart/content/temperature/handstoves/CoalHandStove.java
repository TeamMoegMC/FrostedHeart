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
	public final int max_fuel=3200;
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
		list.add(GuiUtils.translateTooltip("handstove.trash_ash").mergeStyle(TextFormatting.GRAY));
		list.add(GuiUtils.translateTooltip("handstove.ash",this.getAshAmount(stack)/1600).mergeStyle(TextFormatting.GRAY));
		list.add(GuiUtils.translateTooltip("handstove.fuel",this.getFuelAmount(stack),max_fuel).mergeStyle(TextFormatting.GRAY));
    }

    @Override
    public float compute(ItemStack stack, float bodyTemp, float environmentTemp) {
		int fuel=this.getFuelAmount(stack);
		if(fuel>=2) {
			int ash=this.getAshAmount(stack);
			if(ash<=max_fuel) {
				fuel-=2;
				ash+=2;
				this.setFuelAmount(stack, fuel);
				this.setAshAmount(stack, ash);
		        if (bodyTemp > 0) {
		            return this.getMax(stack);
		        }
			}
		}
        return 0;
    }

    public int getAshAmount(ItemStack is) {
    	return is.getOrCreateTag().getInt("ash");
    }
    public int getFuelAmount(ItemStack is) {
    	return is.getOrCreateTag().getInt("fuel");
    }
    public void setAshAmount(ItemStack is,int v) {
    	is.getOrCreateTag().putInt("ash",v);
    }
    public void setFuelAmount(ItemStack is,int v) {
    	is.getOrCreateTag().putInt("fuel",v);
    }
    @Override
	public double getDurabilityForDisplay(ItemStack stack) {
		return this.getFuelAmount(stack)*1.0D/max_fuel;
	}
	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.BLOCK;
	}
	ResourceLocation ashitem=new ResourceLocation("frostedheart","ash");
	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
		ItemStack stack = playerIn.getHeldItem(handIn);
		ActionResult<ItemStack> FAIL = new ActionResult<>(ActionResultType.FAIL,stack);
		if(this.getAshAmount(playerIn.getHeldItem(handIn))>=1600) {
			playerIn.setActiveHand(handIn);
			return new ActionResult<>(ActionResultType.SUCCESS, stack);
		}
		return FAIL; 
	}
	@Override
	public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
		int ash=this.getAshAmount(stack);
		if(ash>=1600) {
			ITag<Item> item=TagCollectionManager.getManager().getItemTags().get(ashitem);
			this.setAshAmount(stack,ash-1600);
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
        return this.getFuelAmount(stack)>0?0.01F:0;
    }
}

package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.client.util.GuiUtils;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DrinkHelper;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;
@Mixin(CowEntity.class)
public abstract class CowEntityMixin extends AnimalEntity {
	private final static ResourceLocation cow_feed=new ResourceLocation(FHMain.MODID,"cow_feed");
	byte feeded;
	int digestTimer;
	byte milk;
	protected CowEntityMixin(EntityType<? extends AnimalEntity> type, World worldIn) {
		super(type, worldIn);
	}
	@Override
	public void tick() {
		super.tick();
		if(digestTimer>0) {
			digestTimer--;
			if(digestTimer==0) {
				if(feeded>0) {
					feeded--;
					milk++;
				}
			}
		}else if(feeded>0) {
			digestTimer=14400;
		}
		
	}
	@Override
	public void writeAdditional(CompoundNBT compound) {
		super.writeAdditional(compound);
		compound.putByte("milk_stored",milk);
		compound.putByte("feed_stored",feeded);
		compound.putInt("feed_digest",digestTimer);
	}
	@Override
	public void readAdditional(CompoundNBT compound) {
		super.readAdditional(compound);
		milk=compound.getByte("milk_stored");
		feeded=compound.getByte("feed_stored");
		digestTimer=compound.getInt("feed_digest");
	}
	/**
	 * @author khjxiaogu
	 * @reason change to our own milk logic
	 * */
	@Override
	@Overwrite
	public ActionResultType getEntityInteractionResult(PlayerEntity playerIn, Hand hand) {
	    ItemStack itemstack = playerIn.getHeldItem(hand);
	    
	    if(!this.isChild()&&!itemstack.isEmpty()&&itemstack.getItem().getTags().contains(cow_feed)) {
	    	if(feeded<2&&milk<2) {
	    		ActionResultType parent=ActionResultType.PASS;
	    		if(this.isBreedingItem(itemstack))
	    			parent=super.getEntityInteractionResult(playerIn, hand);
	    		if(!parent.isSuccessOrConsume())
	    			this.consumeItemFromStack(playerIn, itemstack);
	    		feeded++;
	    		return ActionResultType.func_233537_a_(this.world.isRemote);
	    	}
	    }
	    
	    if (itemstack.getItem() == Items.BUCKET ) {
	    	if(milk>0&& !this.isChild()) {
		       playerIn.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
		       ItemStack itemstack1 = DrinkHelper.fill(itemstack, playerIn, Items.MILK_BUCKET.getDefaultInstance());
		       playerIn.setHeldItem(hand, itemstack1);
		       milk--;
		       return ActionResultType.func_233537_a_(this.world.isRemote);
	    	}
	    	if(!world.isRemote) {
		    	if(feeded<=0)
		    		playerIn.sendMessage(GuiUtils.translateMessage("cow.nomilk.hungry"),playerIn.getUniqueID());
		    	else
		    		playerIn.sendMessage(GuiUtils.translateMessage("cow.nomilk.digest"),playerIn.getUniqueID());
	    	}
	    }
		return super.getEntityInteractionResult(playerIn, hand);
	 }
}

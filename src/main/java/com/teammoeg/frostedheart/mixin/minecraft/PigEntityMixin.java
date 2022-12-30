package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.util.IFeedStore;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
@Mixin(PigEntity.class)
public abstract class PigEntityMixin extends AnimalEntity implements IFeedStore{
	protected PigEntityMixin(EntityType<? extends AnimalEntity> type, World worldIn) {
		super(type, worldIn);
	}


	byte feeded = 0;


	@Inject(at = @At("HEAD"), method = "writeAdditional")
	public void fh$writeAdditional(CompoundNBT compound, CallbackInfo cbi) {
		compound.putByte("feed_stored", feeded);

	}

	@Inject(at = @At("HEAD"), method = "writeAdditional")
	public void fh$readAdditional(CompoundNBT compound, CallbackInfo cbi) {
		feeded = compound.getByte("feed_stored");
	}


	@Inject(at=@At("HEAD"),method="getEntityInteractionResult",cancellable=true)
	public void fh$getEntityInteractionResult(PlayerEntity playerIn, Hand hand,CallbackInfoReturnable<ActionResultType> cbi) {
		ItemStack itemstack = playerIn.getHeldItem(hand);

		if (!itemstack.isEmpty() && isBreedingItem(itemstack)) {
			if (feeded < 2) {
				feeded++;
				if (!this.world.isRemote)
					this.consumeItemFromStack(playerIn, itemstack);
				cbi.setReturnValue(ActionResultType.func_233537_a_(this.world.isRemote));
			}
		}
	}

	@Override
	public boolean consumeFeed() {
		if(feeded>0) {
			feeded--;
			return true;
		}
		return false;
	}
}

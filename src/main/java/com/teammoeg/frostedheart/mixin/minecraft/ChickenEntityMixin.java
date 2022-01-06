package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.teammoeg.frostedheart.FHMain;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;
@Mixin(ChickenEntity.class)
public abstract class ChickenEntityMixin extends AnimalEntity {
	private final static ResourceLocation chicken_feed = new ResourceLocation(FHMain.MODID, "chicken_feed");
	protected ChickenEntityMixin(EntityType<? extends AnimalEntity> type, World worldIn) {
		super(type, worldIn);
	}
	@Shadow
	public int timeUntilNextEgg;
	byte feeded;
	int digestTimer;
	byte egg;
	@Inject(at=@At(value="INVOKE",target="Lnet/minecraft/entity/passive/ChickenEntity;playSound(Lnet/minecraft/util/SoundEvent;FF)V"),method="livingTick",cancellable=true)
	public void fh$layegg(CallbackInfo cbi) {
		if(egg>0) {
			egg--;
			this.playSound(SoundEvents.ENTITY_CHICKEN_EGG, 1.0F, (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
	        this.entityDropItem(Items.EGG);
	        this.timeUntilNextEgg = this.rand.nextInt(6000) + 6000;
		}else if(feeded>0) {
			this.timeUntilNextEgg = 3000;
		}else
			this.timeUntilNextEgg = 28800;
        cbi.cancel();
	}
	@Inject(at=@At("HEAD"),method="writeAdditional")
	public void fh$writeAdditional(CompoundNBT compound,CallbackInfo cbi) {
		compound.putByte("egg_stored", egg);
		compound.putByte("feed_stored", feeded);
		compound.putInt("feed_digest", digestTimer);
	}

	@Inject(at=@At("HEAD"),method="writeAdditional")
	public void fh$readAdditional(CompoundNBT compound,CallbackInfo cbi) {
		egg = compound.getByte("egg_stored");
		feeded = compound.getByte("feed_stored");
		digestTimer = compound.getInt("feed_digest");
	}
	@Override
	public void tick() {
		super.tick();
		if (digestTimer > 0) {
			digestTimer--;
			if (digestTimer == 0) {
				if (feeded > 0) {
					feeded--;
					if(egg<4)
						egg++;
				}
			}
		} else if (feeded > 0) {
			digestTimer = 6000;
		}

	}
	/**
	 * @author khjxiaogu
	 * @reason change to our own milk logic
	 */
	@Override
	public ActionResultType getEntityInteractionResult(PlayerEntity playerIn, Hand hand) {
		ItemStack itemstack = playerIn.getHeldItem(hand);

		if (!this.isChild() && !itemstack.isEmpty() && itemstack.getItem().getTags().contains(chicken_feed)) {
			if (feeded< 4) {
				ActionResultType parent = ActionResultType.PASS;
				if (this.isBreedingItem(itemstack))
					parent = super.getEntityInteractionResult(playerIn, hand);
				if (!parent.isSuccessOrConsume()&&!this.world.isRemote)
					this.consumeItemFromStack(playerIn, itemstack);
				feeded++;
				return ActionResultType.func_233537_a_(this.world.isRemote);
			}
		}
		return super.getEntityInteractionResult(playerIn, hand);
	}
}

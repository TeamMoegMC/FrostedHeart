package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.client.util.GuiUtils;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.MooshroomEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
@Mixin(MooshroomEntity.class)
public abstract class MooshroomMixin extends AnimalEntity {

	public MooshroomMixin(EntityType<? extends CowEntity> p_i48567_1_, World p_i48567_2_) {
		super(p_i48567_1_, p_i48567_2_);
	}

	@Inject(at=@At("HEAD"),method="getEntityInteractionResult",cancellable=true)
	public void fhmo$getEntityInteractionResult(PlayerEntity playerIn, Hand hand,CallbackInfoReturnable<ActionResultType> cbi) {
		ItemStack itemstack = playerIn.getHeldItem(hand);
		if (itemstack.getItem() == Items.BOWL && !this.isChild()) {
			CowEntityMixin ot=(CowEntityMixin)(Object) this;
			
			if(ot.milk<=0) {
				if (!world.isRemote) {
					if (ot.feeded <= 0)
						playerIn.sendMessage(GuiUtils.translateMessage("cow.nomilk.hungry"), playerIn.getUniqueID());
					else
						playerIn.sendMessage(GuiUtils.translateMessage("cow.nomilk.digest"), playerIn.getUniqueID());
				}
				cbi.setReturnValue(ActionResultType.PASS);
			}
			ot.milk--;
		}
	}

}

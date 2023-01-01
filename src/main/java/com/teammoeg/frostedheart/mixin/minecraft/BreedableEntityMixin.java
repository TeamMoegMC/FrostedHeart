package com.teammoeg.frostedheart.mixin.minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.teammoeg.frostedheart.util.BreedUtil;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.passive.horse.LlamaEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
@Mixin({PigEntity.class,ChickenEntity.class,FoxEntity.class,RabbitEntity.class,CatEntity.class,LlamaEntity.class,AbstractHorseEntity.class})
public abstract class BreedableEntityMixin extends AnimalEntity {

	protected BreedableEntityMixin(EntityType<? extends AnimalEntity> type, World worldIn) {
		super(type, worldIn);
	}
	@Inject(at = @At("HEAD"), method = "isBreedingItem",cancellable=true)
    public void isBreedingItem(ItemStack itemStack,CallbackInfoReturnable<Boolean> cbi) {
        EntityType<?> type = getType();
        boolean f=BreedUtil.isBreedingItem(type, itemStack);
        if(f)
        	cbi.setReturnValue(true);
     }
}

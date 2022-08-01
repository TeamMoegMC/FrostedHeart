package com.teammoeg.frostedheart.mixin.minecraft;

import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AnimalEntity.class)
public abstract class AnimalEntityMixin extends AgeableEntity {

    protected AnimalEntityMixin(EntityType<? extends AgeableEntity> type, World worldIn) {
        super(type, worldIn);
    }

    @ModifyConstant(method = "spawnBabyAnimal", constant = @Constant(intValue = 6000))
    public int getBreedCooldown(int orig) {
        return 28800;
    }
}

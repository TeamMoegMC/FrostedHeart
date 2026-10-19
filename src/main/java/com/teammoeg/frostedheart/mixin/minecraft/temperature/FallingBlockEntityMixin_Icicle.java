/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import com.teammoeg.frostedheart.content.climate.block.IcicleBlock;
import com.teammoeg.frostedheart.content.climate.block.IcicleFalling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin_Icicle {
    @Shadow private BlockState blockState;
    @Shadow private boolean hurtEntities;
    @Shadow private float fallDamagePerDistance;
    @Shadow private int fallDamageMax;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void frostedheart$fallingIce(CallbackInfo ci) {
        if (!IcicleBlock.isIcicle(blockState)) return;
        IcicleFalling.tick((FallingBlockEntity) (Object) this);
        ci.cancel();
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void frostedheart$iceDamage(float distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> ci) {
        if (!IcicleBlock.isIcicle(blockState)) return;
        FallingBlockEntity self = (FallingBlockEntity) (Object) this;
        if (hurtEntities && !self.level().isClientSide) {
            float damage = IcicleFalling.damage(distance, fallDamagePerDistance, fallDamageMax);
            if (damage > 0) for (var target : self.level().getEntities(self, self.getBoundingBox(),
                    EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(EntitySelector.LIVING_ENTITY_STILL_ALIVE)))
                target.hurt(self.damageSources().fallingStalactite(self), damage);
        }
        ci.setReturnValue(false);
    }
}

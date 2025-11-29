package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Wolf.class)
public abstract class WolfMixin extends TamableAnimal implements NeutralMob {

    protected WolfMixin(EntityType<? extends TamableAnimal> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    /**
     * @author yuesha-yc
     * @reason Make wolves angry in darkness
     */
    @Override
    public boolean isAngryAt(LivingEntity pTarget) {
        Wolf self = (Wolf)(Object)this;

        // Check if it's dark enough
        float lightLevel = self.getLightLevelDependentMagicValue();
        // In darkness, wolf becomes angry if it can attack the target
        // However, it should not attack any Player if is already tamed
        if (lightLevel < 0.5F && !isTame()) {
            return self.canAttack(pTarget);
        }

        // Otherwise use the default NeutralMob behavior
        if (!this.canAttack(pTarget)) {
            return false;
        } else {
            return pTarget.getType() == EntityType.PLAYER && this.isAngryAtAllPlayers(pTarget.level())
                    ? true
                    : pTarget.getUUID().equals(this.getPersistentAngerTarget());
        }
    }

    /**
     * Injects into the createAttributes method to increase wolf stats
     */
    @Inject(method = "createAttributes", at = @At("RETURN"), cancellable = true)
    private static void frostedheart$enhanceAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        AttributeSupplier.Builder builder = Mob.createMobAttributes();

        // Modify the attributes with enhanced values
        builder.add(Attributes.MAX_HEALTH, 16.0D)       // Base: 8.0D
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.45F)     // Base: 0.3F
                .add(Attributes.ATTACK_DAMAGE, 4.0D);     // Base: 2.0D

        // Return the modified builder
        cir.setReturnValue(builder);
    }

    /**
     * Modify the values when a wolf is tamed to make it even stronger
     */
    @Inject(method = "setTame", at = @At("TAIL"))
    private void frostedheart$enhanceTamedAttributes(boolean tamed, CallbackInfo ci) {
        Wolf wolf = (Wolf)(Object)this;

        if (tamed) {
            // Enhance tamed wolf stats even further
            wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(32.0D);      // Base tamed: 20.0D
            wolf.setHealth(32.0F);
        } else {
            wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(16.0D);
        }

        wolf.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6.0D);
    }
}

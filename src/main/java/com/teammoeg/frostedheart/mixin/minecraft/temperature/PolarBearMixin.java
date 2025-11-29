package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PolarBear.class)
public abstract class PolarBearMixin implements NeutralMob {

    /**
     * @author yuesha-yc
     * @reason Make bear angry in day
     */
    @Override
    public boolean isAngryAt(LivingEntity pTarget) {
        PolarBear self = (PolarBear)(Object)this;

        // Check if it's dark enough
        float lightLevel = self.getLightLevelDependentMagicValue();
        if (lightLevel > 0.5F) {
            // In day time, bear becomes angry if it can attack the target
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
        builder.add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D);

        // Return the modified builder
        cir.setReturnValue(builder);
    }
}

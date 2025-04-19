package com.teammoeg.frostedheart.mixin.minecraft.temperature;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Wolf.class)
public abstract class WolfMixin implements NeutralMob {

    /**
     * @author yuesha-yc
     * @reason Make wolves angry in darkness
     */
    @Override
    public boolean isAngryAt(LivingEntity pTarget) {
        Wolf self = (Wolf)(Object)this;

        // Check if it's dark enough
        float lightLevel = self.getLightLevelDependentMagicValue();
        if (lightLevel < 0.5F) {
            // In darkness, wolf becomes angry if it can attack the target
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
}

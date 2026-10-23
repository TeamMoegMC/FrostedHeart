/* Copyright (c) 2026 TeamMoeg */
package com.teammoeg.frostedheart.mixin.minecraft.misc;

import com.teammoeg.frostedheart.content.climate.block.IcicleEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** These vanilla effects bypass PlayLevelSoundEvent on the server. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin_IcicleVibrations {
    @Inject(method = "levelEvent", at = @At("TAIL"))
    private void frostedheart$levelVibration(Player player, int event, BlockPos pos, int data, CallbackInfo ci) {
        IcicleEvents.levelEvent((ServerLevel) (Object) this, event, pos, data);
    }

    @Inject(method = "broadcastEntityEvent", at = @At("TAIL"))
    private void frostedheart$fireworkVibration(Entity entity, byte event, CallbackInfo ci) {
        if (event == 17 && entity instanceof FireworkRocketEntity rocket)
            IcicleEvents.fireworkBurst((ServerLevel) (Object) this, rocket);
    }
}

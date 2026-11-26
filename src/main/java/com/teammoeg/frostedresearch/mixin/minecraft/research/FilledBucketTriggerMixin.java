package com.teammoeg.frostedresearch.mixin.minecraft.research;

import com.teammoeg.frostedresearch.knowledge.KnowledgeBehaviorBridge;
import net.minecraft.advancements.critereon.FilledBucketTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FilledBucketTrigger.class)
public abstract class FilledBucketTriggerMixin {
    @Inject(method = "trigger", at = @At("TAIL"))
    private void frostedresearch$bucketFilled(ServerPlayer player, ItemStack filled, CallbackInfo ci) {
        KnowledgeBehaviorBridge.bucketFilled(player, filled);
    }
}

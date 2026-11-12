package com.teammoeg.frostedresearch.mixin.minecraft.research;

import com.teammoeg.frostedresearch.knowledge.KnowledgeBehaviorBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class KnowledgeItemUseAcquisitionMixin {
    @Unique private ItemStack frostedresearch$usedOn;
    @Inject(method = "useOn", at = @At("HEAD"))
    private void frostedresearch$before(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (context.getPlayer() instanceof ServerPlayer) frostedresearch$usedOn = ((ItemStack)(Object)this).copy();
    }
    @Inject(method = "useOn", at = @At("RETURN"))
    private void frostedresearch$after(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        var used = frostedresearch$usedOn; frostedresearch$usedOn = null;
        if (used != null && cir.getReturnValue().consumesAction() && context.getPlayer() instanceof ServerPlayer player)
            KnowledgeBehaviorBridge.itemUsed(player, used);
    }
}

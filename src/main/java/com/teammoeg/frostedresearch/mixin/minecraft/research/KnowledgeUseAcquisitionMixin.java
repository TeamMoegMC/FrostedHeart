package com.teammoeg.frostedresearch.mixin.minecraft.research;

import com.teammoeg.frostedresearch.knowledge.*;
import com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeElement;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationSampling;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;

/** Successful server actions only; cancelled/PASS callbacks never count as a use. */
@Mixin(ServerPlayerGameMode.class)
public abstract class KnowledgeUseAcquisitionMixin {
    @Unique private ItemStack frostedresearch$used = ItemStack.EMPTY;
    @Inject(method = "useItem", at = @At("HEAD"))
    private void frostedresearch$before(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        frostedresearch$used = stack.copy();
    }
    @Inject(method = "useItem", at = @At("RETURN"))
    private void frostedresearch$item(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (cir.getReturnValue().consumesAction() && !frostedresearch$used.isEmpty() && KnowledgeBehaviorBridge.interested(IdeaAcquisitionService.ITEM_USED))
            IdeaAcquisitionService.emit(player, IdeaAcquisitionService.ITEM_USED, Optional.of(ItemRetrieval.behaviorSample(player, frostedresearch$used)));
    }
    @Inject(method = "useItemOn", at = @At("HEAD"))
    private void frostedresearch$beginBlock(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        KnowledgeBehaviorBridge.beginUse();
    }
    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void frostedresearch$block(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        KnowledgeBehaviorBridge.finishUse(player, cir.getReturnValue().consumesAction() && KnowledgeBehaviorBridge.interested(IdeaAcquisitionService.BLOCK_INTERACTED) ? Optional.of(KnowledgeElement.observation(ObservationSampling.block(player, hit.getBlockPos()))) : Optional.empty());
    }
}

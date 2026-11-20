package com.teammoeg.frostedresearch.mixin.minecraft.research;

import com.teammoeg.frostedheart.bootstrap.reference.FHTags;
import com.teammoeg.frostedresearch.knowledge.*;
import com.teammoeg.frostedresearch.knowledge.item.ItemRetrieval;
import com.teammoeg.frostedresearch.knowledge.model.KnowledgeElement;
import com.teammoeg.frostedresearch.knowledge.observation.ObservationSampling;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import java.util.*;

/** Successful server actions only; cancelled/PASS callbacks never count as a use. */
@Mixin(ServerPlayerGameMode.class)
public abstract class KnowledgeUseAcquisitionMixin {
    @Shadow @Final protected ServerPlayer player;
    @Unique private static final ResourceLocation frostedresearch$oreMined = new ResourceLocation("frostedheart", "ore_mined_after_copper");
    @Unique private static final ResourceLocation frostedresearch$copper = new ResourceLocation("create", "copper");
    @Unique private ItemStack frostedresearch$used = ItemStack.EMPTY;

    // Forge adds this method to ServerPlayerGameMode and retains its name in production.
    @Inject(method = "removeBlock", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD, remap = false)
    private void frostedresearch$mined(BlockPos pos, boolean canHarvest, CallbackInfoReturnable<Boolean> cir, BlockState original) {
        if (!cir.getReturnValue() || player instanceof FakePlayer || !original.is(FHTags.Blocks.ORES.tag)
                || !KnowledgeBehaviorBridge.interested(frostedresearch$oreMined)) return;
        var advancement = player.server.getAdvancements().getAdvancement(frostedresearch$copper);
        if (advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone())
            IdeaAcquisitionService.emit(player, frostedresearch$oreMined, Optional.empty());
    }

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

/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 *
 * Frosted Heart is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * Frosted Heart is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Frosted Heart. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.teammoeg.frostedheart.mixin.client.stratadrill;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.frostedheart.content.stratadrill.StrataDrillAssembly;
import com.teammoeg.frostedheart.content.stratadrill.client.StrataDrillHands;
import com.teammoeg.frostedheart.content.stratadrill.client.StrataDrillHeldRenderer;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Two narrow holder-aware adapters, leaving the vanilla render pass, model and other items alone. */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin_StrataDrill {
    @Shadow private ItemStack offHandItem;

    // The vanilla height mixes attack recovery (including block-break abort) into re-equipping.
    // Drill work has its own pose; only remove the attack contribution to this visual height.
    // Vanilla's actual slot-change and hands-busy branches still lower and raise the item.
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getAttackStrengthScale(F)F"))
    private float fh$strataVisualAttackHeight(LocalPlayer player, float partialTick) {
        return StrataDrillAssembly.isDrill(player.getMainHandItem()) ? 1.0F
                : player.getAttackStrengthScale(partialTick);
    }

    // Oculus can cancel this method at HEAD when it is processing the other hand pass.
    // Do not move this to RenderHandEvent or HEAD; those can run ahead of the pass filter.
    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0), cancellable = true)
    private void fh$strataFirstPerson(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand,
                                    float swing, ItemStack stack, float equip, PoseStack poses,
                                    MultiBufferSource buffers, int light, CallbackInfo callback) {
        if (StrataDrillHands.firstPerson(player, hand, stack, this.offHandItem, partialTick, equip,
                poses, buffers, light)) {
            callback.cancel();
        }
    }

    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
    private void fh$strataThirdPerson(LivingEntity holder, ItemStack stack, ItemDisplayContext display,
                                    boolean left, PoseStack poses, MultiBufferSource buffers, int light,
                                    CallbackInfo callback) {
        if (holder instanceof AbstractClientPlayer player
                && StrataDrillHeldRenderer.thirdPerson(player, stack, display, left, poses, buffers, light)) {
            callback.cancel();
        }
    }
}

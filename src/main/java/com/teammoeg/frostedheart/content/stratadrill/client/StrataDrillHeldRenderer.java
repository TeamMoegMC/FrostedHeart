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

package com.teammoeg.frostedheart.content.stratadrill.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.teammoeg.frostedheart.content.stratadrill.StrataDrillAssembly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeRenderTypes;

/** Standard accumulated item rendering with rigid transforms for the V3.7 working pieces. */
public final class StrataDrillHeldRenderer {
    private static final float MODEL_UNIT = 1 / 16.0F;
    private static final float TAU = Mth.TWO_PI;
    // Wide-head axle follows the enlarged wheel, measured from the unchanged chuck interface.
    private static final float WHEEL_AXLE_X = -5.66F;
    // Center of the raised diesel side-drive, shared by the flywheel and opposite shaft end.
    private static final float DIESEL_AXLE_X = 9.10F;
    private static final float DIESEL_AXLE_Y = 9.70F;
    private static final float CRANK_X = 9.55F;
    private static final float CRANK_Y = 7.65F;
    // Allowed by V3.7 cylinder/piston overlap at both ends: 0.86 model units total stroke.
    private static final float CRANK_RADIUS = 0.43F;
    private static final float ROD_LENGTH = 2.64F;

    private StrataDrillHeldRenderer() {}

    public static boolean thirdPerson(AbstractClientPlayer player, ItemStack stack, ItemDisplayContext display,
                               boolean left, PoseStack poses, MultiBufferSource buffers, int packedLight) {
        if (display != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                && display != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) return false;
        if (!StrataDrillAssembly.isDrill(stack) || stack != player.getMainHandItem() || player.isSpectator()) return false;
        float partialTick = Minecraft.getInstance().getFrameTime();
        float phase = StrataDrillClientWork.phase(player, partialTick);
        if (Float.isNaN(phase)) return false;
        StrataDrillAssembly assembly = StrataDrillAssembly.read(stack);
        if (assembly == null) return false;
        StrataDrillModels.AssemblyModels models = StrataDrillModels.get(assembly, stack);
        if (models == null) return false;
        poses.pushPose();
        StrataDrillHands.applyWorkingThirdPersonGrip(poses, left, player, partialTick);
        poses.translate(-0.5F, -0.5F, -0.5F);
        draw(models, stack, phase, poses, buffers, packedLight);
        poses.popPose();
        return true;
    }

    static void draw(StrataDrillModels.AssemblyModels models, ItemStack stack, float phase,
                     PoseStack poses, MultiBufferSource buffers, int light) {
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        // S04 textures are all opaque. Use the active pass's buffer; the owner handles all flushing.
        VertexConsumer vertex = ItemRenderer.getFoilBufferDirect(buffers, ForgeRenderTypes.ITEM_LAYERED_SOLID.get(),
                true, stack.hasFoil());
        if (Float.isNaN(phase) || phase == 0) {
            render(renderer, models.complete(), stack, poses, vertex, light);
            return;
        }
        render(renderer, models.fixed(), stack, poses, vertex, light);
        float angle = phase * TAU;
        poses.pushPose();
        if (models.wheel()) {
            poses.rotateAround(Axis.ZP.rotation(angle), WHEEL_AXLE_X * MODEL_UNIT, 0.5F, 0.5F);
        } else {
            poses.rotateAround(Axis.XP.rotation(angle * 2), 0, 0.5F, 0.5F);
        }
        render(renderer, models.head(), stack, poses, vertex, light);
        poses.popPose();

        poses.pushPose();
        if (models.steam()) {
            poses.rotateAround(Axis.ZP.rotation(angle), CRANK_X * MODEL_UNIT, CRANK_Y * MODEL_UNIT, 10.6F * MODEL_UNIT);
        } else {
            poses.rotateAround(Axis.ZP.rotation(angle * 2), DIESEL_AXLE_X * MODEL_UNIT,
                    DIESEL_AXLE_Y * MODEL_UNIT, 10.69F * MODEL_UNIT);
        }
        render(renderer, models.shaft(), stack, poses, vertex, light);
        poses.popPose();

        if (models.steam()) {
            float pinX = CRANK_RADIUS * (Mth.cos(angle) - 1);
            float pinY = CRANK_RADIUS * Mth.sin(angle);
            float run = Mth.sqrt(ROD_LENGTH * ROD_LENGTH - pinY * pinY);
            poses.pushPose();
            poses.translate(pinX * MODEL_UNIT, pinY * MODEL_UNIT, 0);
            poses.rotateAround(Axis.ZP.rotation((float) Math.atan2(-pinY, run)),
                    (CRANK_X + CRANK_RADIUS) * MODEL_UNIT, CRANK_Y * MODEL_UNIT, 11.04F * MODEL_UNIT);
            render(renderer, models.rod(), stack, poses, vertex, light);
            poses.popPose();

            poses.pushPose();
            poses.translate((pinX + run - ROD_LENGTH) * MODEL_UNIT, 0, 0);
            render(renderer, models.piston(), stack, poses, vertex, light);
            poses.popPose();
        }
    }

    private static void render(ItemRenderer renderer, BakedModel model, ItemStack stack, PoseStack poses,
                               VertexConsumer vertex, int light) {
        // This exact entry also lets the installed Embeddium fast path take over when supported.
        renderer.renderModelLists(model, stack, light, OverlayTexture.NO_OVERLAY, poses, vertex);
    }
}

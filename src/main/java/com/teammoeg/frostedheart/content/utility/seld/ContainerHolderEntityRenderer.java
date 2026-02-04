/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.utility.seld;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class ContainerHolderEntityRenderer<T extends ContainerHolderEntity> extends EntityRenderer<T> {

    private final ContainerHolderModel<ContainerHolderEntity> containerHolderModel;
    private final ResourceLocation textures;

    public ContainerHolderEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4F;
        this.textures =  new ResourceLocation(FHMain.MODID + ":textures/entity/" + "oak_sled" + ".png");
        this.containerHolderModel = new ContainerHolderModel<>(context.bakeLayer(ContainerHolderModel.CONTAINER_HOLDER));
    }

    @Override
    public void render(T entity, float yRot, float partialTicks, PoseStack poseStack, MultiBufferSource pBuffer, int pPackedLight) {

        Entity vehicle = entity.getVehicle();
        if( vehicle == null || (vehicle.isControlledByLocalInstance()
                && Minecraft.getInstance().options.getCameraType().isFirstPerson())) return;

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.325D, 0.0D);

        //hack cause I can't get the rotation to align. god darn. I spent so much time trying to get it to work so this wil have to do
        float xRot = vehicle.getViewXRot(partialTicks);
        yRot = vehicle.getViewYRot(partialTicks);

       poseStack.mulPose(Axis.YP.rotationDegrees(270.0F - yRot));
//       poseStack.mulPose(Axis.XN.rotationDegrees(xRot));

        float f = (float) entity.getHurtTime() - partialTicks;
        float f1 = entity.getDamage() - partialTicks;
        if (f1 < 0.0F) {
            f1 = 0.0F;
        }

        if (f > 0.0F) {
           poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(f) * f * f1 / 10.0F));
        }


        BlockState blockstate = entity.getDisplayState();
        if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
            poseStack.pushPose();

            poseStack.scale(-1.0F, -1.0F, 1.0F);
            poseStack.translate(-0.5D, 0, 0);
//            blockRenderer.renderSingleBlock(blockstate, poseStack, pBuffer, pPackedLight, OverlayTexture.NO_OVERLAY);

            VertexConsumer vertexConsumer = pBuffer.getBuffer(RenderType.entityCutout(getTextureLocation(entity)));
            containerHolderModel.renderToBuffer(poseStack, vertexConsumer, pPackedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

            poseStack.popPose();
        }
        poseStack.popPose();

    }
    @Override
    public ResourceLocation getTextureLocation(ContainerHolderEntity entity) {
        return this.textures;
    }

}

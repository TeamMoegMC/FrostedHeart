/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.client.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import blusunrize.immersiveengineering.mixin.accessors.client.ModelAccess;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.LivingEntity;

public class HeaterVestModel<T extends LivingEntity> extends FHArmorBaseModel<T> {
    private final ModelRenderer front;
    private final ModelRenderer deco;
    private final ModelRenderer back;

    public HeaterVestModel(float modelSize, float yOffsetIn, int textureWidthIn, int textureHeightIn) {
        super(modelSize, yOffsetIn, textureWidthIn, textureHeightIn);
        ((ModelAccess) this).setRenderType(RenderType::getEntityTranslucent);

        textureWidth = 32;
        textureHeight = 32;

        front = new ModelRenderer(this);
        front.setRotationPoint(0.0F, 12.0F, 0.0F);
        front.setTextureOffset(0, 16).addBox(-2.5F, -9.75F, -4.5F, 5.0F, 6.0F, 2.0F, 0.0F, false);
        bipedBody.addChild(front);

        deco = new ModelRenderer(this);
        deco.setRotationPoint(0.0F, -1.0F, 0.0F);
        front.addChild(deco);
        setRotationAngle(deco, -0.3927F, 0.0F, 0.0F);
        deco.setTextureOffset(14, 16).addBox(-3.0F, -3.0F, -5.25F, 6.0F, 3.0F, 2.0F, 0.0F, false);

        back = new ModelRenderer(this);
        back.setRotationPoint(0.0F, 12.0F, 0.0F);
        back.setTextureOffset(0, 0).addBox(-4.0F, -12.0F, -3.0F, 8.0F, 12.0F, 4.0F, 0.0F, false);
        bipedBody.addChild(back);

        this.bipedHead.showModel = false;
        this.bipedHeadwear.showModel = false;
        this.bipedLeftArm.showModel = false;
        this.bipedRightArm.showModel = false;
        this.bipedLeftLeg.showModel = false;
        this.bipedRightLeg.showModel = false;
    }

    public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }

    @Override
    public void render(MatrixStack matrixStackIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
        super.render(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
    }

    static HeaterVestModel modelInstance;

    public static HeaterVestModel getModel() {
        if (modelInstance == null)
            modelInstance = new HeaterVestModel(.0625f, 0, 32, 32);
        return modelInstance;
    }
}

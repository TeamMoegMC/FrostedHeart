/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.client.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import com.teammoeg.frostedheart.util.client.ClientUtils;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.util.math.MathHelper;

public class FHArmorBaseModel<T extends LivingEntity> extends BipedModel<T> {
    T entityTemp;

    public FHArmorBaseModel(float modelSize, float yOffsetIn, int textureWidthIn, int textureHeightIn) {
        super(modelSize, yOffsetIn, textureWidthIn, textureHeightIn);
    }

    @Override
    public void renderToBuffer(MatrixStack matrixStackIn, IVertexBuilder bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
        if (entityTemp != null) {
            young = entityTemp.isBaby();
            crouching = entityTemp.isShiftKeyDown();
            riding = entityTemp.isPassenger() && (entityTemp.getVehicle() != null && entityTemp.getVehicle().shouldRiderSit());
        }
        super.renderToBuffer(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        entityTemp = entity;
        attackTime = entity.getAttackAnim(ClientUtils.partialTicks());
        if (entity instanceof ArmorStandEntity)
            setRotationAnglesStand(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        else if (entity instanceof SkeletonEntity || entity instanceof ZombieEntity)
            setRotationAnglesZombie(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        else
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    public void setRotationAnglesStand(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity instanceof ArmorStandEntity) {
            ArmorStandEntity entityarmorstand = (ArmorStandEntity) entity;
            this.head.xRot = (0.01745329F * entityarmorstand.getHeadPose().getX());
            this.head.yRot = (0.01745329F * entityarmorstand.getHeadPose().getY());
            this.head.zRot = (0.01745329F * entityarmorstand.getHeadPose().getZ());
            this.head.setPos(0.0F, 1.0F, 0.0F);
            this.body.xRot = (0.01745329F * entityarmorstand.getBodyPose().getX());
            this.body.yRot = (0.01745329F * entityarmorstand.getBodyPose().getY());
            this.body.zRot = (0.01745329F * entityarmorstand.getBodyPose().getZ());
            this.leftArm.xRot = (0.01745329F * entityarmorstand.getLeftArmPose().getX());
            this.leftArm.yRot = (0.01745329F * entityarmorstand.getLeftArmPose().getY());
            this.leftArm.zRot = (0.01745329F * entityarmorstand.getLeftArmPose().getZ());
            this.rightArm.xRot = (0.01745329F * entityarmorstand.getRightArmPose().getX());
            this.rightArm.yRot = (0.01745329F * entityarmorstand.getRightArmPose().getY());
            this.rightArm.zRot = (0.01745329F * entityarmorstand.getRightArmPose().getZ());
            this.leftLeg.xRot = (0.01745329F * entityarmorstand.getLeftLegPose().getX());
            this.leftLeg.yRot = (0.01745329F * entityarmorstand.getLeftLegPose().getY());
            this.leftLeg.zRot = (0.01745329F * entityarmorstand.getLeftLegPose().getZ());
            this.leftLeg.setPos(1.9F, 11.0F, 0.0F);
            this.rightLeg.xRot = (0.01745329F * entityarmorstand.getRightLegPose().getX());
            this.rightLeg.yRot = (0.01745329F * entityarmorstand.getRightLegPose().getY());
            this.rightLeg.zRot = (0.01745329F * entityarmorstand.getRightLegPose().getZ());
            this.rightLeg.setPos(-1.9F, 11.0F, 0.0F);
            this.hat.copyFrom(this.head);
        }
    }

    public void setRotationAnglesZombie(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float f6 = MathHelper.sin(this.attackTime * 3.141593F);
        float f7 = MathHelper.sin((1.0F - (1.0F - this.attackTime) * (1.0F - this.attackTime)) * 3.141593F);
        this.rightArm.zRot = 0.0F;
        this.leftArm.zRot = 0.0F;
        this.rightArm.yRot = (-(0.1F - f6 * 0.6F));
        this.leftArm.yRot = (0.1F - f6 * 0.6F);
        this.rightArm.xRot = -1.570796F;
        this.leftArm.xRot = -1.570796F;
        this.rightArm.xRot -= f6 * 1.2F - f7 * 0.4F;
        this.leftArm.xRot -= f6 * 1.2F - f7 * 0.4F;
        this.rightArm.zRot += MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.leftArm.zRot -= MathHelper.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.rightArm.xRot += MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
        this.leftArm.xRot -= MathHelper.sin(ageInTicks * 0.067F) * 0.05F;
    }
}

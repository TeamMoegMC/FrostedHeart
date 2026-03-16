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

package com.teammoeg.chorda.client.model;

import java.util.function.Function;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teammoeg.chorda.client.ClientUtils;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.util.Mth;

/**
 * 自定义盔甲模型。扩展人形模型以支持盔甲架、僵尸和骷髅的特殊姿态动画。
 * <p>
 * Custom armor model. Extends the humanoid model to support special pose animations for armor stands, zombies, and skeletons.
 *
 * @param <T> 穿戴盔甲的生物实体类型 / the type of living entity wearing the armor
 */
public class CArmorModel<T extends LivingEntity> extends HumanoidModel<T> {
    T entityTemp;



    public CArmorModel(ModelPart pRoot, Function<ResourceLocation, RenderType> pRenderType) {
		super(pRoot, pRenderType);
	}

	public CArmorModel(ModelPart pRoot) {
		super(pRoot);
	}

	/**
	 * 渲染模型到缓冲区，渲染前同步实体的年龄、蹲伏和骑乘状态。
	 * <p>
	 * Renders the model to the buffer, syncing the entity's baby, crouching, and riding states before rendering.
	 */
	@Override
    public void renderToBuffer(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
        if (entityTemp != null) {
            young = entityTemp.isBaby();
            crouching = entityTemp.isShiftKeyDown();
            riding = entityTemp.isPassenger() && (entityTemp.getVehicle() != null && entityTemp.getVehicle().shouldRiderSit());
        }
        super.renderToBuffer(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
    }

    /**
     * 设置模型动画。根据实体类型分别处理盔甲架、僵尸/骷髅和普通实体的动画。
     * <p>
     * Sets up model animations. Dispatches to different animation handlers based on entity type
     * (armor stand, zombie/skeleton, or normal entities).
     */
    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        entityTemp = entity;
        attackTime = entity.getAttackAnim(ClientUtils.partialTicks());
        if (entity instanceof ArmorStand)
            setRotationAnglesStand(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        else if (entity instanceof Skeleton || entity instanceof Zombie)
            setRotationAnglesZombie(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        else
            super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }

    /**
     * 设置盔甲架的旋转角度，从盔甲架的姿态数据中读取各部位旋转。
     * <p>
     * Sets rotation angles for armor stands by reading pose data from the armor stand entity.
     *
     * @param entity 盔甲架实体 / the armor stand entity
     * @param limbSwing 肢体摆动量 / the limb swing amount
     * @param limbSwingAmount 肢体摆动幅度 / the limb swing amplitude
     * @param ageInTicks 实体年龄刻 / the entity age in ticks
     * @param netHeadYaw 头部水平旋转 / the net head yaw
     * @param headPitch 头部俯仰角 / the head pitch
     */
    public void setRotationAnglesStand(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity instanceof ArmorStand) {
            ArmorStand entityarmorstand = (ArmorStand) entity;
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

    /**
     * 设置僵尸/骷髅的旋转角度，手臂前伸并添加攻击动画。
     * <p>
     * Sets rotation angles for zombies/skeletons with arms extended forward and attack animations applied.
     *
     * @param entity 僵尸或骷髅实体 / the zombie or skeleton entity
     * @param limbSwing 肢体摆动量 / the limb swing amount
     * @param limbSwingAmount 肢体摆动幅度 / the limb swing amplitude
     * @param ageInTicks 实体年龄刻 / the entity age in ticks
     * @param netHeadYaw 头部水平旋转 / the net head yaw
     * @param headPitch 头部俯仰角 / the head pitch
     */
    public void setRotationAnglesZombie(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        float f6 = Mth.sin(this.attackTime * 3.141593F);
        float f7 = Mth.sin((1.0F - (1.0F - this.attackTime) * (1.0F - this.attackTime)) * 3.141593F);
        this.rightArm.zRot = 0.0F;
        this.leftArm.zRot = 0.0F;
        this.rightArm.yRot = (-(0.1F - f6 * 0.6F));
        this.leftArm.yRot = (0.1F - f6 * 0.6F);
        this.rightArm.xRot = -1.570796F;
        this.leftArm.xRot = -1.570796F;
        this.rightArm.xRot -= f6 * 1.2F - f7 * 0.4F;
        this.leftArm.xRot -= f6 * 1.2F - f7 * 0.4F;
        this.rightArm.zRot += Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.leftArm.zRot -= Mth.cos(ageInTicks * 0.09F) * 0.05F + 0.05F;
        this.rightArm.xRot += Mth.sin(ageInTicks * 0.067F) * 0.05F;
        this.leftArm.xRot -= Mth.sin(ageInTicks * 0.067F) * 0.05F;
    }
}

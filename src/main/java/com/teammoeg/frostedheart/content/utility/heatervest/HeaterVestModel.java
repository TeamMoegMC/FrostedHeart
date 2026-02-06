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

package com.teammoeg.frostedheart.content.utility.heatervest;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teammoeg.chorda.client.model.CArmorModel;
import com.teammoeg.frostedheart.FHMain;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

public class HeaterVestModel<T extends LivingEntity> extends CArmorModel<T> {
	public static final ModelLayerLocation HEATER_VEST_LAYER=new ModelLayerLocation(FHMain.rl("heater_vest"), "main");
    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
    	PartDefinition root=mesh.getRoot();
    	PartDefinition body=root.getChild("body");
    	body.addOrReplaceChild("front", CubeListBuilder.create().texOffs(0, 16).addBox(-2.5F, -9.75F, -4.5F, 5.0F, 6.0F, 2.0F, false), PartPose.offset(0, 12f, 0));
    	body.addOrReplaceChild("deco", CubeListBuilder.create().texOffs(14, 16).addBox(-3.0F, -3.0F, -5.25F, 6.0F, 3.0F, 2.0F, false), PartPose.offsetAndRotation(0, -1f, 0,-0.3927F, 0, 0));
    	body.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -12.0F, -3.0F, 8.0F, 12.0F, 4.0F, false), PartPose.offset(0, 12f, 0));
    	return LayerDefinition.create(mesh, 32, 32);
    }
    public HeaterVestModel(ModelPart root) {
        super(root, RenderType::entityTranslucent);

        this.head.visible = false;
        this.hat.visible = false;
        this.leftArm.visible = false;
        this.rightArm.visible = false;
        this.leftLeg.visible = false;
        this.rightLeg.visible = false;
    }

    @Override
    public void renderToBuffer(PoseStack matrixStackIn, VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
        super.renderToBuffer(matrixStackIn, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
    }

}

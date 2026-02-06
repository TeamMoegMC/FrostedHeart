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

package com.teammoeg.frostedheart.content.utility.seld;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class ContainerHolderModel<T extends ContainerHolderEntity> extends EntityModel<T> {

    public static final ModelLayerLocation CONTAINER_HOLDER = new ModelLayerLocation(
            new ResourceLocation(FHMain.MODID,"container_holder"),"main");
        private final ModelPart chest;
        private final ModelPart rope;
        private final ModelPart rope2;

    public ContainerHolderModel(ModelPart root) {
        this.chest = root.getChild("chest");
        this.rope = this.chest.getChild("rope");
        this.rope2 = this.chest.getChild("rope2");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition chest = partdefinition.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 15).addBox(14.975F, -17.0F, 4.025F, 14.0F, 10.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(-11.975F, 24.0F, -11.025F));

        PartDefinition rope = chest.addOrReplaceChild("rope", CubeListBuilder.create().texOffs(0, 63).addBox(23.4108F, -13.9059F, -3.975F, 1.0F, 1.0F, 16.0F, new CubeDeformation(-0.025F))
                .texOffs(50, 39).addBox(23.4108F, -1.0F, -5.4608F, 1.0F, 1.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.4358F, -4.0941F, 7.0F));

        PartDefinition cube_r1 = rope.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(34, 77).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(24.4108F, -13.9059F, 12.025F, -1.4399F, 0.0F, 0.0F));

        PartDefinition cube_r2 = rope.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(34, 67).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(23.9108F, -3.0F, 13.4608F, 0.0401F, 0.0F, 0.0F));

        PartDefinition cube_r3 = rope.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(34, 67).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(23.9108F, -3.0F, -5.4108F, -0.0244F, 0.0F, 0.0F));

        PartDefinition cube_r4 = rope.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(34, 77).addBox(-1.0F, 0.0F, -11.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(24.4108F, -13.9059F, -3.975F, 1.4399F, 0.0F, 0.0F));

        PartDefinition rope2 = chest.addOrReplaceChild("rope2", CubeListBuilder.create().texOffs(0, 63).addBox(23.4108F, -13.9059F, -3.975F, 1.0F, 1.0F, 16.0F, new CubeDeformation(-0.025F))
                .texOffs(50, 39).addBox(23.4108F, -1.0F, -5.4608F, 1.0F, 1.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offset(2.5642F, -4.0941F, 7.0F));

        PartDefinition cube_r5 = rope2.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(34, 77).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(24.4108F, -13.9059F, 12.025F, -1.4399F, 0.0F, 0.0F));

        PartDefinition cube_r6 = rope2.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(34, 67).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(23.9108F, -3.0F, 13.4608F, 0.0401F, 0.0F, 0.0F));

        PartDefinition cube_r7 = rope2.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(34, 67).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(23.9108F, -3.0F, -5.4108F, -0.0244F, 0.0F, 0.0F));

        PartDefinition cube_r8 = rope2.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(34, 77).addBox(-1.0F, 0.0F, -11.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(24.4108F, -13.9059F, -3.975F, 1.4399F, 0.0F, 0.0F));
        return LayerDefinition.create(meshdefinition, 128, 128);
    }
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        chest.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

}

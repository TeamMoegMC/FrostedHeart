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
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;

public class SledModel<T extends SledEntity> extends EntityModel<T> {
    public static final ModelLayerLocation SLED_LAYER = new ModelLayerLocation(
            new ResourceLocation(FHMain.MODID,"sled_layer"),"main");
    public static final ModelLayerLocation QUILT_LAYER = new ModelLayerLocation(
            new ResourceLocation(FHMain.MODID,"quilt_layer"),"main");

    private final ModelPart board;
/*    private final ModelPart chest;
    private final ModelPart rope;
    private final ModelPart rope2;*/
    private final ModelPart left;
    private final ModelPart right;

    public SledModel(ModelPart root) {
        this.board = root.getChild("board");
/*        this.chest = root.getChild("chest");
        this.rope = this.chest.getChild("rope");
        this.rope2 = this.chest.getChild("rope2");*/
        this.left = root.getChild("left");
        this.right = root.getChild("right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition board = partdefinition.addOrReplaceChild("board", CubeListBuilder.create().texOffs(0, 39).addBox(-22.25F, -2.0F, -0.525F, 2.0F, 1.0F, 23.0F, new CubeDeformation(0.0F))
                .texOffs(0, 9).addBox(-14.0F, -3.0F, 6.475F, 35.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 9).addBox(-14.0F, -3.0F, 11.475F, 35.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(50, 59).addBox(18.0F, -4.0F, 2.475F, 2.0F, 1.0F, 17.0F, new CubeDeformation(0.0F))
                .texOffs(0, 4).addBox(-15.0F, -3.0F, 2.475F, 37.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(0, 4).addBox(-15.0F, -3.0F, 16.475F, 37.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
                .texOffs(56, 15).addBox(-8.0F, -1.0F, 1.975F, 2.0F, 1.0F, 18.0F, new CubeDeformation(-0.025F))
                .texOffs(56, 15).addBox(12.0F, -1.0F, 1.975F, 2.0F, 1.0F, 18.0F, new CubeDeformation(-0.025F))
                .texOffs(56, 15).addBox(2.0F, -1.0F, 1.975F, 2.0F, 1.0F, 18.0F, new CubeDeformation(-0.025F)), PartPose.offset(-1.0F, 20.0F, -10.975F));

//        PartDefinition chest = partdefinition.addOrReplaceChild("chest", CubeListBuilder.create().texOffs(0, 15).addBox(14.975F, -17.0F, 4.025F, 14.0F, 10.0F, 14.0F, new CubeDeformation(0.0F)), PartPose.offset(-11.975F, 24.0F, -11.025F));

/*
        PartDefinition rope = chest.addOrReplaceChild("rope", CubeListBuilder.create().texOffs(0, 63).addBox(23.4108F, -13.9059F, -3.975F, 1.0F, 1.0F, 16.0F, new CubeDeformation(-0.025F))
                .texOffs(50, 39).addBox(23.4108F, -1.0F, -5.4608F, 1.0F, 1.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offset(-6.4358F, -4.0941F, 7.0F));

        PartDefinition cube_r1 = rope.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(34, 77).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(24.4108F, -13.9059F, 12.025F, -1.4399F, 0.0F, 0.0F));

        PartDefinition cube_r2 = rope.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(34, 67).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(23.9108F, -3.0F, 13.4608F, 0.0401F, 0.0F, 0.0F));

        PartDefinition cube_r3 = rope.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(34, 67).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(23.9108F, -3.0F, -5.4108F, -0.0244F, 0.0F, 0.0F));

        PartDefinition cube_r4 = rope.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(34, 77).addBox(-1.0F, 0.0F, -11.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(24.4108F, -13.9059F, -3.975F, 1.4399F, 0.0F, 0.0F));
*/

/*
        PartDefinition rope2 = chest.addOrReplaceChild("rope2", CubeListBuilder.create().texOffs(0, 63).addBox(23.4108F, -13.9059F, -3.975F, 1.0F, 1.0F, 16.0F, new CubeDeformation(-0.025F))
                .texOffs(50, 39).addBox(23.4108F, -1.0F, -5.4608F, 1.0F, 1.0F, 19.0F, new CubeDeformation(0.0F)), PartPose.offset(2.5642F, -4.0941F, 7.0F));

        PartDefinition cube_r5 = rope2.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(34, 77).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(24.4108F, -13.9059F, 12.025F, -1.4399F, 0.0F, 0.0F));

        PartDefinition cube_r6 = rope2.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(34, 67).addBox(-0.5F, 0.0F, -1.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(23.9108F, -3.0F, 13.4608F, 0.0401F, 0.0F, 0.0F));

        PartDefinition cube_r7 = rope2.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(34, 67).addBox(-0.5F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(23.9108F, -3.0F, -5.4108F, -0.0244F, 0.0F, 0.0F));

        PartDefinition cube_r8 = rope2.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(34, 77).addBox(-1.0F, 0.0F, -11.0F, 1.0F, 1.0F, 11.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(24.4108F, -13.9059F, -3.975F, 1.4399F, 0.0F, 0.0F));
*/

        PartDefinition left = partdefinition.addOrReplaceChild("left", CubeListBuilder.create().texOffs(0, 0).addBox(-12.725F, 5.0F, 0.25F, 39.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.275F, 18.0F, -11.0F));

        PartDefinition cube_r9 = left.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(58, 77).addBox(0.05F, 0.0F, -6.0F, 2.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(58, 77).addBox(10.05F, 0.0F, -6.0F, 2.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(58, 77).addBox(-9.95F, 0.0F, -6.0F, 2.0F, 1.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.225F, -0.0341F, 2.2412F, 1.309F, 0.0F, 0.0F));

        PartDefinition cube_r10 = left.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(56, 34).addBox(-4.95F, -1.0F, -2.0F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.025F)), PartPose.offsetAndRotation(-12.772F, 5.9829F, 2.25F, 0.0F, 0.0F, 0.3491F));

        PartDefinition cube_r11 = left.addOrReplaceChild("cube_r11", CubeListBuilder.create().texOffs(34, 63).addBox(-4.0F, -1.0F, -2.0F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-17.4235F, 4.2899F, 2.25F, 0.0F, 0.0F, 0.9599F));

        PartDefinition right = partdefinition.addOrReplaceChild("right", CubeListBuilder.create().texOffs(0, 0).addBox(-12.725F, 5.0F, -3.25F, 39.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(-3.275F, 18.0F, 11.0F));

        PartDefinition cube_r12 = right.addOrReplaceChild("cube_r12", CubeListBuilder.create().texOffs(58, 77).addBox(0.05F, 0.0F, -1.0F, 2.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(58, 77).addBox(10.05F, 0.0F, -1.0F, 2.0F, 1.0F, 7.0F, new CubeDeformation(0.0F))
                .texOffs(58, 77).addBox(-9.95F, 0.0F, -1.0F, 2.0F, 1.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(4.225F, -0.0341F, -2.2412F, -1.309F, 0.0F, 0.0F));

        PartDefinition cube_r13 = right.addOrReplaceChild("cube_r13", CubeListBuilder.create().texOffs(56, 34).addBox(-4.95F, -1.0F, -1.0F, 5.0F, 1.0F, 3.0F, new CubeDeformation(0.025F)), PartPose.offsetAndRotation(-12.772F, 5.9829F, -2.25F, 0.0F, 0.0F, 0.3491F));

        PartDefinition cube_r14 = right.addOrReplaceChild("cube_r14", CubeListBuilder.create().texOffs(34, 63).addBox(-4.0F, -1.0F, -1.0F, 4.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-17.4235F, 4.2899F, -2.25F, 0.0F, 0.0F, 0.9599F));

        return LayerDefinition.create(meshdefinition, 128, 128);
    }
    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        board.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
//        chest.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        left.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        right.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }
}
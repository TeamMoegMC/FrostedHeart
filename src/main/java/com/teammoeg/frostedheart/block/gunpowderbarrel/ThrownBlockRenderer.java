package com.teammoeg.frostedheart.block.gunpowderbarrel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;

public class ThrownBlockRenderer extends EntityRenderer<ThrowableItemProjectile> {
    private final BlockRenderDispatcher blockRenderer;

    public ThrownBlockRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(ThrowableItemProjectile entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        if (entity.getItem().getItem() instanceof BlockItem blockItem) {
            var state = blockItem.getBlock().defaultBlockState();
            if (state.getRenderShape() != RenderShape.MODEL) return;

            poseStack.pushPose();
            poseStack.translate(0, 0.5, 0);

            Vec3 motion = entity.getDeltaMovement();
            float speed = (float) motion.length();
            float rollSpeedFactor = 10;
            float roll = (entity.tickCount + partialTick) * speed * rollSpeedFactor;

            poseStack.mulPose(Axis.YP.rotationDegrees(entity.getViewYRot(partialTick)));
            poseStack.mulPose(Axis.XP.rotationDegrees(entity.getViewXRot(partialTick)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));

            poseStack.translate(-0.5, -0.5, -0.5);
            blockRenderer.renderSingleBlock(state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            poseStack.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ThrowableItemProjectile entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

package com.teammoeg.frostedheart.content.utility.seld;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
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

    private final BlockRenderDispatcher blockRenderer;

    public ContainerHolderEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4F;
        this.blockRenderer = Minecraft.getInstance().getBlockRenderer();
    }

    @Override
    public void render(T entity, float yRot, float partialTicks, PoseStack poseStack, MultiBufferSource pBuffer, int pPackedLight) {

        Entity vehicle = entity.getVehicle();
        if( vehicle == null || (vehicle.isControlledByLocalInstance()
                && Minecraft.getInstance().options.getCameraType().isFirstPerson())) return;

        poseStack.pushPose();

        //hack cause I can't get the rotation to align. god darn. I spent so much time trying to get it to work so this wil have to do
        float xRot = vehicle.getViewXRot(partialTicks);
        yRot = vehicle.getViewYRot(partialTicks);

       poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yRot));
       poseStack.mulPose(Axis.XN.rotationDegrees(xRot));

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
            float scale = 0.75F;

            poseStack.scale(scale, scale, scale);
            poseStack.translate(-0.5D, 0, -0.5D);
            //poseStack.mulPose(Vector3f.YP.rotationDegrees(90.0F));
            blockRenderer.renderSingleBlock(blockstate, poseStack, pBuffer, pPackedLight, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        poseStack.popPose();

    }

    /**
     * Returns the location of an entity's texture.
     */
    @Override
    public ResourceLocation getTextureLocation(T pEntity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

}

package com.teammoeg.frostedheart.content.utility.seld;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.stream.Stream;

public class SledEntityRenderer extends EntityRenderer<SledEntity> {

    private final ResourceLocation textures;
    private final ResourceLocation quiltTextures;
    private final SledModel<SledEntity> model;
//    private final SledModel<SledEntity> modelBamboo;
    private final QuiltModel<SledEntity> quiltModel;

    public SledEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.8F;
        this.model = new SledModel<>(context.bakeLayer(SledModel.SLED_LAYER));
//        this.modelBamboo = new SledModel<>(context.bakeLayer(ClientRegistry.SLED_MODEL_BAMBOO));
        this.quiltModel = new QuiltModel<>(context.bakeLayer(SledModel.QUILT_LAYER));
  /*      this.textures = WoodTypeRegistry.getTypes().stream().collect(ImmutableMap.toImmutableMap((e) -> e,
                (t) -> SnowySpirit.res("textures/entity/sled/" + t.getTexturePath() + ".png")));*/
        this.textures =  new ResourceLocation(FHMain.MODID + ":textures/entity/" + "oak_sled" + ".png");
        this.quiltTextures = new ResourceLocation(FHMain.MODID + ":textures/entity/gray_quilt" + ".png");/*Stream.of(DyeColor.values()).collect(ImmutableMap.toImmutableMap((e) -> e,
                (t) -> new ResourceLocation(FHMain.MODID + ":textures/entity/sled/quilt/" + t.getName() + ".png")))*/
    }

    @Override
    public void render(SledEntity sled, float yRot, float partialTicks, PoseStack poseStack, MultiBufferSource bufferSource, int light) {

        double dy = Mth.lerp(partialTicks, sled.prevAdditionalY, sled.cachedAdditionalY);
        poseStack.pushPose();

        poseStack.translate(0.0D, 1.525D+ dy, 0.0D);

        //same stuff that happens to yRot when its created
        float xRot = sled.getViewXRot(partialTicks);

        poseStack.mulPose(Axis.YP.rotationDegrees(270.0F - yRot));
//        poseStack.mulPose(Axis.ZN.rotationDegrees(xRot));
        float hurtTme = (float) sled.getHurtTime() - partialTicks;
        float damage = sled.getDamage() - partialTicks;
        if (damage < 0.0F) {
            damage = 0.0F;
        }

        float zRot = 0;
        if (hurtTme > 0.0F) {
            zRot = Mth.sin(hurtTme) * hurtTme * damage / 10.0F * (float) sled.getHurtDir();
            poseStack.mulPose(Axis.ZP.rotationDegrees(zRot));
        }

        poseStack.pushPose();
        poseStack.translate(-0, 0.125, 0.4);
        poseStack.scale(0.75F, 0.75F, 0.75F);
        poseStack.translate(-0.5, 0, -0.5);

        poseStack.popPose();

        ResourceLocation resourcelocation = this.getTextureLocation(sled);

        poseStack.scale(-1.0F, -1.0F, 1.0F);

        VertexConsumer vertexconsumer = bufferSource.getBuffer(model.renderType(resourcelocation));
        var mod = /*resourcelocation.getPath().equals("textures/entity/sled/bamboo.png") ? modelBamboo :*/ model;
        mod.renderToBuffer(poseStack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        DyeColor color = sled.getSeatType();
        if (color != null) {
            vertexconsumer = bufferSource.getBuffer(model.renderType(this.quiltTextures));
            quiltModel.renderToBuffer(poseStack, vertexconsumer, light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        }

        poseStack.popPose();

        this.renderLeash(sled, partialTicks, poseStack, bufferSource,
                (float) Math.toRadians(90 + yRot),
                (float) Math.toRadians(xRot),
                (float) Math.toRadians(zRot),
                dy);

        super.render(sled, yRot, partialTicks, poseStack, bufferSource, light);

        if (this.entityRenderDispatcher.shouldRenderHitBoxes()) {
            this.renderDebugHitbox(poseStack, bufferSource.getBuffer(RenderType.lines()), sled, partialTicks);
        }


    }

    @Override
    public ResourceLocation getTextureLocation(SledEntity sled) {
        return this.textures;
    }

    private void renderDebugHitbox(PoseStack pMatrixStack, VertexConsumer pBuffer, SledEntity pEntity, float pPartialTicks) {
        AABB aabb = pEntity.getBoundingBox().move(lerpV(pPartialTicks, pEntity.prevProjectedPos, pEntity.projectedPos))
                .move(-pEntity.getX(), -pEntity.getY(), -pEntity.getZ());
        LevelRenderer.renderLineBox(pMatrixStack, pBuffer, aabb, 1.0F, 0, 0, 1.0F);

        if (pEntity.hasPuller()) {
            aabb = pEntity.pullerAABB.move(-pEntity.getX(), -pEntity.getY(), -pEntity.getZ());
            LevelRenderer.renderLineBox(pMatrixStack, pBuffer, aabb, 0, 1, 0, 1.0F);
        }

        Vec3 movement = lerpV(pPartialTicks, pEntity.prevDeltaMovement, pEntity.getDeltaMovement());

        Matrix4f matrix4f = pMatrixStack.last().pose();
        Matrix3f matrix3f = pMatrixStack.last().normal();
        float mult = 6;
        float eye = (pEntity.getEyeHeight() + 1 + pEntity.cachedAdditionalY);
        pBuffer.vertex(matrix4f, 0.0F, eye, 0.0F)
                .color(0, 255, 0, 255)
                .normal(matrix3f, (float) movement.x, (float) movement.y, (float) movement.z).endVertex();
        pBuffer.vertex(matrix4f, (float) (movement.x * mult), (float) (eye + movement.y * mult), (float) (movement.z * mult))
                .color(0, 255, 0, 255)
                .normal(matrix3f, (float) movement.x, (float) movement.y, (float) movement.z).endVertex();


        pBuffer.vertex(matrix4f, 0.0F, eye + 0.25f, 0.0F)
                .color(255, 0, 255, 255)
                .normal(matrix3f, 0, 1, 0).endVertex();
        pBuffer.vertex(matrix4f, 0, (float) (eye + 0.25f + pEntity.misalignedFrictionFactor), 0)
                .color(255, 0, 255, 255)
                .normal(matrix3f, 0, 1, 0).endVertex();


        if (pEntity.boost) {
            movement = movement.normalize().scale(-1);
            pBuffer.vertex(matrix4f, 0.0F, eye, 0.0F)
                    .color(255, 255, 0, 255)
                    .normal(matrix3f, (float) movement.x, (float) movement.y, (float) movement.z).endVertex();
            pBuffer.vertex(matrix4f, (float) (movement.x), (float) (eye + movement.y), (float) (movement.z))
                    .color(255, 255, 0, 255)
                    .normal(matrix3f, (float) movement.x, (float) movement.y, (float) movement.z).endVertex();
        }


    }

    public static Vec3 lerpV(float delta, Vec3 start, Vec3 end) {
        return new Vec3(
                Mth.lerp(delta, start.x, end.x),
                Mth.lerp(delta, start.y, end.y),
                Mth.lerp(delta, start.z, end.z));
    }


    private void renderLeash(SledEntity sled, float pPartialTicks, PoseStack poseStack, MultiBufferSource pBuffer,
                             float yRot, float xRot, float zRot, double addY) {
        Animal wolf = sled.getSledPuller();
        if (wolf != null) {
            boolean bear = ForgeRegistries.ENTITY_TYPES.getKey(wolf.getType()).getPath().equals("grizzly_bear");/*Utils.getID(wolf.getType()).getPath().equals("grizzly_bear")*/
            Vec3 wolfPos = wolf.getRopeHoldPosition(pPartialTicks).add(0, wolf.isBaby() ? 0.1 : 0.25, 0);

            float bbw = wolf.getBbWidth() / (2.875f + (bear ? 1.5f : 0));
            Vec3 sledOffset = new Vec3(0.4125f, 0, 0.95f);
            Vec3 ropeOffset = new Vec3(bbw, 0, 0);

            //yaw
            float cos = Mth.cos(yRot);
            float sin = Mth.sin(yRot);

            //slope
            float pCos = Mth.cos(xRot);
            float pSin = Mth.sin(xRot);


            //wobble
            float wSin = Mth.sin(zRot);

            double sledX = Mth.lerp(pPartialTicks, sled.xo, sled.getX());
            double sledY = Mth.lerp(pPartialTicks, sled.yo, sled.getY());
            double sledZ = Mth.lerp(pPartialTicks, sled.zo, sled.getZ());

            BlockPos sledEyePos = BlockPos.containing(sled.getEyePosition(pPartialTicks));
            BlockPos wolfEyePos = BlockPos.containing(wolf.getEyePosition(pPartialTicks));

            for (int rope = -1; rope <= 1; rope += 2) {
                VertexConsumer vertexconsumer = pBuffer.getBuffer(RenderType.leash());
                poseStack.pushPose();


                double wolfOffsetX = cos * ropeOffset.z + sin * ropeOffset.x * rope;
                double wolfOffsetZ = sin * ropeOffset.z - cos * ropeOffset.x * rope;
                double offsetX = (cos * sledOffset.z + sin * sledOffset.x * rope) * pCos;
                double offsetZ = (sin * sledOffset.z - cos * sledOffset.x * rope) * pCos;
                double offsetY = -pSin * sledOffset.length() + 0.25 + addY -
                        sledOffset.x * wSin * rope;
                double pX = sledX + offsetX;
                double pY = sledY + offsetY;
                double pZ = sledZ + offsetZ;
                poseStack.translate(offsetX, offsetY, offsetZ);

                float deltaX = (float) (wolfPos.x + wolfOffsetX - pX);
                float deltaY = (float) (wolfPos.y - pY);
                float deltaZ = (float) (wolfPos.z + wolfOffsetZ - pZ);
                float width = 0.025F;

                Matrix4f matrix4f = poseStack.last().pose();
                float f4 = Mth.invSqrt(deltaX * deltaX + deltaZ * deltaZ) * width / 2.0F;
                //id what these are for but something with angle
                float mathZ = deltaZ * f4;

                float mathX = deltaX * f4;

                int blockLight0 = this.getBlockLightLevel(sled, sledEyePos);
                Level level = wolf.level();
                int blockLight1 = wolf.isOnFire() ? 15 : level.getBrightness(LightLayer.BLOCK, wolfEyePos);
                int skyLight0 = level.getBrightness(LightLayer.SKY, sledEyePos);
                int skyLight1 = level.getBrightness(LightLayer.SKY, wolfEyePos);

                //each lead is composed of 2 strips
                int maxSegments = 12;
                for (int index = 0; index <= maxSegments; ++index) {
                    addVertexPair(vertexconsumer, matrix4f, deltaX, deltaY, deltaZ,
                            blockLight0, blockLight1, skyLight0, skyLight1,
                            0.025F, 0.025F, mathZ, mathX, index,
                            false, maxSegments);
                }

                for (int index = maxSegments; index >= 0; --index) {
                    addVertexPair(vertexconsumer, matrix4f, deltaX, deltaY, deltaZ,
                            blockLight0, blockLight1, skyLight0, skyLight1,
                            0.025F, 0.0F, mathZ, mathX, index,
                            true, maxSegments);
                }

                poseStack.popPose();

                if (pBuffer instanceof MultiBufferSource.BufferSource bu) {
                    bu.endBatch();
                }
            }
        }
    }

    //stolen from leash renderer
    private static void addVertexPair(VertexConsumer vertexConsumer, Matrix4f matrix4f,
                                      float startX, float startY, float startZ,
                                      int blockLight0, int blockLight1, int skyLight0, int skyLight1,
                                      float y0, float y1,
                                      float dx, float dz, int index,
                                      boolean flippedColors, int maxSegments) {
        float segment = index / (float) maxSegments;
        int i = (int) Mth.lerp(segment, blockLight0, blockLight1);
        int j = (int) Mth.lerp(segment, skyLight0, skyLight1);
        int light = LightTexture.pack(i, j);
        float darker = index % 2 == (flippedColors ? 1 : 0) ? 0.7F : 1.0F;
        //hardcoded colors 0.0
        float red = 0.5F * darker;
        float green = 0.4F * darker;
        float blue = 0.3F * darker;
        float sx = startX * segment;
        //parable here
        float sy = startY > 0.0F ? startY * segment * segment : startY - startY * (1.0F - segment) * (1.0F - segment);
        float sz = startZ * segment;
        vertexConsumer.vertex(matrix4f, sx - dx, sy + y1, sz + dz)
                .color(red, green, blue, 1.0F).uv2(light).endVertex();
        vertexConsumer.vertex(matrix4f, sx + dx, sy + y0 - y1, sz - dz)
                .color(red, green, blue, 1.0F).uv2(light).endVertex();
    }
    public class QuiltModel<T extends SledEntity> extends EntityModel<T> {

        private final ModelPart quilt;

        public QuiltModel(ModelPart root) {
            this.quilt = root.getChild("quilt");
        }

        public static LayerDefinition createBodyLayer() {
            MeshDefinition meshdefinition = new MeshDefinition();
            PartDefinition partdefinition = meshdefinition.getRoot();

            PartDefinition quilt = partdefinition.addOrReplaceChild("quilt", CubeListBuilder.create()
                            .texOffs(0, 0)
                            .addBox(-7.0F, -11.5F, -3.0F, 14.0F, 20.0F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, -1.5708F, 0.0F, 0.0F));

            return LayerDefinition.create(meshdefinition, 32, 32);
        }

        @Override
        public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
            quilt.render(poseStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
        }
    }
}

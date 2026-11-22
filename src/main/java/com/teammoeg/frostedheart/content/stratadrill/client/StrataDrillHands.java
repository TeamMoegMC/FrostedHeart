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

package com.teammoeg.frostedheart.content.stratadrill.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.teammoeg.chorda.client.pose.HeldGripPose;
import com.teammoeg.chorda.client.pose.HeldGripPose.Point;
import com.teammoeg.frostedheart.content.stratadrill.StrataDrillAssembly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/** S04 grip profile. Uses actual V3.7 wood grip centers and the ordinary player skin and sleeves. */
public final class StrataDrillHands {
    // V3.7 grip coordinates (editor position minus 8, in sixteenths).
    private static final Point REAR_PALM_1P = new Point(0.7109375F, -0.0228125F, 0);
    // Left palm on the outer end of the existing wood, just inboard of the metal stop.
    private static final Point FRONT_PALM_RIGHT_1P = new Point(-0.303125F, -0.021875F, 0.388125F);
    private static final Point FRONT_PALM_LEFT_1P = new Point(-0.31875F, -0.021875F, 0.24F);
    private static final float FIRST_SCALE = 0.82F;
    private static final float THIRD_SCALE = 0.70F;
    private static final float MAIN_PALM_DEPTH = -1.18F;
    private static final float SUPPORT_PALM_DEPTH = -1.15F;
    // Exact wrist axes from ItemInHandLayer, which normally uses the lower, outer face of the arm.
    private static final Quaternionf ITEM_LAYER_AXES = new Quaternionf().rotationX(-Mth.HALF_PI).rotateY(Mth.PI);
    // Player-model axes: +Y down, -Z forward. The two profiles use opposite flanks, but the
    // real auxiliary grip is asymmetric: the left-primary carry reaches its inner wood end.
    // At the larger scale keep the casing clear of the jacket, and take the front grip from
    // its upper wood rather than lengthening the original arm or reaching through the end cap.
    private static final ThirdProfile THIRD_RIGHT = new ThirdProfile(1, -8.15F, 9.88F, 1.17F, -1.90F,
            15, 6.5F, 0, new Point(0.68125F, -0.01875F, -0.020625F),
            new Point(-0.28375F, 0.011875F, 0.4075F));
    private static final ThirdProfile THIRD_LEFT = new ThirdProfile(-1, 6.30F, 9.24F, 5.19F, -1.97F,
            26.5F, 10, -13, new Point(0.68125F, -0.01875F, 0.029375F),
            new Point(-0.28375F, 0.011875F, 0.221875F));
    private static final ThirdPose CARRY_RIGHT = new ThirdPose().solve(THIRD_RIGHT, 0);
    private static final ThirdPose CARRY_LEFT = new ThirdPose().solve(THIRD_LEFT, 0);
    // Only used synchronously on the render thread, consumed before any other draw is called.
    private static final ThirdPose WORK_POSE = new ThirdPose();

    private static final HumanoidModel.ArmPose TWO_HANDS =
            HumanoidModel.ArmPose.create("FROSTEDHEART_STRATADRILL_TWO", true, StrataDrillHands::poseBoth);
    private static final HumanoidModel.ArmPose ONE_HAND =
            HumanoidModel.ArmPose.create("FROSTEDHEART_STRATADRILL_ONE", false, StrataDrillHands::posePrimary);

    // PlayerRenderer's vanilla first-person skin methods call getArmPose as part of model setup.
    // In their narrow render scope let them use the vanilla skin-model bind pose.
    private static boolean drawingFirstPersonSkin;
    private static final IClientItemExtensions EXTENSION = new IClientItemExtensions() {
        @Override
        @Nullable
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            if (drawingFirstPersonSkin || !(entity instanceof AbstractClientPlayer player)
                    || player.isSpectator() || player.isSwimming() || player.isFallFlying()
                    || player.isVisuallySwimming() || player.isUsingItem()) return null;
            if (hand == InteractionHand.OFF_HAND) return ONE_HAND;
            return entity.getOffhandItem().isEmpty() ? TWO_HANDS : ONE_HAND;
        }
    };

    private StrataDrillHands() {}

    public static IClientItemExtensions extension() {
        return EXTENSION;
    }

    private static void posePrimary(HumanoidModel<?> model, LivingEntity entity, HumanoidArm primary) {
        poseArms(model, (AbstractClientPlayer) entity, primary, false);
    }

    private static void poseBoth(HumanoidModel<?> model, LivingEntity entity, HumanoidArm primary) {
        poseArms(model, (AbstractClientPlayer) entity, primary, true);
    }

    private static void poseArms(HumanoidModel<?> model, AbstractClientPlayer entity, HumanoidArm primary,
                                 boolean both) {
        float sign = HeldGripPose.side(primary);
        boolean left = primary == HumanoidArm.LEFT;
        float partial = Minecraft.getInstance().getFrameTime();
        ThirdPose grip = primary == entity.getMainArm() ? thirdPose(entity, left, partial) : carry(left);
        ModelPart arm = left ? model.leftArm : model.rightArm;
        arm.setRotation(grip.mainEuler.x, grip.mainEuler.y, grip.mainEuler.z);
        // Vanilla's swing would move the wrist and drill away from the other hand. Replace only
        // S04's own swing, never the swing of an occupied opposite hand.
        if (primary == entity.getMainArm() && entity.swingingArm != InteractionHand.OFF_HAND) {
            model.attackTime = 0;
        }
        keepGripThroughVanillaPose(model, entity, arm, sign);
        if (both) {
            ModelPart support = left ? model.rightArm : model.leftArm;
            support.setRotation(grip.supportEuler.x, grip.supportEuler.y, grip.supportEuler.z);
            support.z = grip.profile.supportShoulderZ;
            keepGripThroughVanillaPose(model, entity, support, -sign);
        }
    }

    private static void keepGripThroughVanillaPose(HumanoidModel<?> model, LivingEntity entity,
                                                   ModelPart arm, float sign) {
        // HumanoidModel adds these after its Forge ArmPose callback. Cancel only the components that
        // would break these two fixed grips; the rest of the player pose is still entirely vanilla.
        if (model.crouching) arm.xRot -= 0.4F;
        AnimationUtils.bobModelPart(arm, entity.tickCount + Minecraft.getInstance().getFrameTime(), -sign);
    }

    static void applyThirdPersonGrip(PoseStack poses, boolean left) {
        applyThirdGrip(poses, carry(left));
    }

    static void applyWorkingThirdPersonGrip(PoseStack poses, boolean left, AbstractClientPlayer actor,
                                            float partial) {
        applyThirdGrip(poses, thirdPose(actor, left, partial));
    }

    private static ThirdPose carry(boolean left) {
        return left ? CARRY_LEFT : CARRY_RIGHT;
    }

    private static ThirdPose thirdPose(AbstractClientPlayer actor, boolean left, float partial) {
        float load = StrataDrillClientWork.loadAmount(actor, partial);
        if (load == 0 || actor.isUsingItem() || actor.isFallFlying() || actor.isVisuallySwimming()) return carry(left);
        return WORK_POSE.solve(left ? THIRD_LEFT : THIRD_RIGHT, load);
    }

    private static void applyThirdGrip(PoseStack poses, ThirdPose grip) {
        // Native mount is (−side, 10, −2) in arm-local pixels. Bring it to the distal wood
        // contact without changing the cuboid or putting the mount at the middle of the forearm.
        poses.translate(0, (-2 - MAIN_PALM_DEPTH) / 16, (10 - grip.mainPalmReach) / 16);
        poses.mulPose(grip.toolInWrist);
        poses.scale(THIRD_SCALE, THIRD_SCALE, THIRD_SCALE);
        grip.profile.rearPalm.placeAtOrigin(poses);
    }

    /** Calibrated low carry in torso space, with a fixed upright cutter direction. */
    private static final class ThirdProfile {
        final float side;
        final float supportShoulderZ;
        final Point rearAtRest;
        final Point rearPalm;
        final Quaternionf toolToModel = new Quaternionf();
        final Vector3f shaftForward = new Vector3f(-1, 0, 0);
        final Vector3f gripSpan = new Vector3f();

        ThirdProfile(float side, float flank, float height, float depth, float supportShoulderZ,
                     float inward, float noseUp, float roll, Point rearPalm, Point frontPalm) {
            this.side = side;
            this.supportShoulderZ = supportShoulderZ;
            this.rearAtRest = new Point(flank, height, depth);
            this.rearPalm = rearPalm;
            toolToModel.rotationY(-side * inward * Mth.DEG_TO_RAD).rotateX(-noseUp * Mth.DEG_TO_RAD)
                    .rotateY(-Mth.HALF_PI).rotateX(Mth.PI + roll * Mth.DEG_TO_RAD);
            toolToModel.transform(shaftForward);
            frontPalm.offsetFrom(rearPalm, toolToModel, THIRD_SCALE * 16, gripSpan);
        }
    }

    /** Rotate original arms about their shoulders to meet a rigid translation of the drill. */
    private static final class ThirdPose {
        final Vector3f mainEuler = new Vector3f();
        final Vector3f supportEuler = new Vector3f();
        final Quaternionf toolInWrist = new Quaternionf();
        private final Quaternionf armRotation = new Quaternionf();
        private final Vector3f rear = new Vector3f();
        private final Vector3f reach = new Vector3f();
        ThirdProfile profile;
        float mainPalmReach;

        ThirdPose solve(ThirdProfile profile, float load) {
            this.profile = profile;
            Point rest = profile.rearAtRest;
            // At full load the tool lifts 0.69 model pixel and advances 0.90 along its own axis.
            // There is no work-dependent yaw, pitch, shoulder slide, or target-facing correction.
            rear.set(rest.x(), rest.y() - 0.69F * load, rest.z()).fma(0.90F * load, profile.shaftForward);
            float side = profile.side;
            reach.set(rear).sub(-5 * side, 2, 0);
            mainPalmReach = orientArm(-side, MAIN_PALM_DEPTH);
            armRotation.getEulerAnglesZYX(mainEuler);
            toolInWrist.set(armRotation).mul(ITEM_LAYER_AXES).invert().mul(profile.toolToModel);
            reach.set(rear).add(profile.gripSpan).sub(5 * side, 2, profile.supportShoulderZ);
            orientArm(side, SUPPORT_PALM_DEPTH);
            armRotation.getEulerAnglesZYX(supportEuler);
            return this;
        }

        private float orientArm(float palmX, float palmZ) {
            // Contact may travel within the distal arm block, not by lengthening the arm. Use
            // the shortest rotation so palm approach does not add artificial wrist twist.
            float palmY = Mth.sqrt(Math.max(0, reach.lengthSquared() - palmX * palmX - palmZ * palmZ));
            armRotation.rotationTo(palmX, palmY, palmZ, reach.x, reach.y, reach.z);
            return palmY;
        }
    }

    /**
     * Called from the actual vanilla arm draw, downstream of Oculus's pass selector. The supplied
     * buffers belong to that pass. Suppression is only for the S04 branch actually drawn here.
     */
    public static boolean firstPerson(AbstractClientPlayer player, InteractionHand hand, ItemStack stack,
                                      ItemStack displayedOffhand, float partialTick, float equipProgress,
                                      PoseStack poses, MultiBufferSource buffers, int light) {
        StrataDrillAssembly assembly = StrataDrillAssembly.read(stack);
        if (assembly == null) return false;
        StrataDrillModels.AssemblyModels models = StrataDrillModels.get(assembly, stack);
        if (models == null) return false;

        boolean main = hand == InteractionHand.MAIN_HAND;
        HumanoidArm primary = main ? player.getMainArm() : player.getMainArm().getOpposite();
        boolean right = primary == HumanoidArm.RIGHT;
        boolean support = main && displayedOffhand.isEmpty() && player.getOffhandItem().isEmpty();
        float phase = main ? StrataDrillClientWork.phase(player, partialTick) : Float.NaN;
        float load = main ? StrataDrillClientWork.loadAmount(player, partialTick) : 0;
        float sign = HeldGripPose.side(primary);
        // Keep the level drill in the operator's carrying lane. The shallow inward angle clears
        // the asymmetric auxiliary handle, not the crosshair, and stays fixed under pressure.
        float yaw = (right ? -80 : -106) * Mth.DEG_TO_RAD;
        Quaternionf rotation = new Quaternionf().rotationY(yaw);
        // Model -X is the drill's working axis. The original two arms follow the same lift and
        // axial movement, rather than aiming, sweeping sideways or chasing the block position.
        float press = 0.095F * load;
        Point rear = new Point(sign * 0.49F - press * Mth.cos(yaw),
                -0.434F + 0.048F * load - 0.59F * HeldGripPose.equipWeight(equipProgress),
                -0.545F + press * Mth.sin(yaw));
        poses.pushPose();
        poses.translate(rear.x(), rear.y(), rear.z());
        poses.mulPose(rotation);
        poses.scale(FIRST_SCALE, FIRST_SCALE, FIRST_SCALE);
        REAR_PALM_1P.placeAtOrigin(poses);
        poses.translate(-0.5F, -0.5F, -0.5F);
        StrataDrillHeldRenderer.draw(models, stack, phase, poses, buffers, light);
        poses.popPose();

        if (!player.isInvisible()
                && Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            boolean previous = drawingFirstPersonSkin;
            drawingFirstPersonSkin = true;
            try {
                // Sample the support palm before the arm draw reuses the quaternion.
                Point supportPalm = support ? viewPalm(rear, REAR_PALM_1P,
                        right ? FRONT_PALM_RIGHT_1P : FRONT_PALM_LEFT_1P, rotation) : null;
                drawSkinArm(renderer, player, primary, rear, false, rotation, poses, buffers, light);
                if (supportPalm != null) {
                    drawSkinArm(renderer, player, primary.getOpposite(), supportPalm, true, rotation, poses, buffers, light);
                }
            } finally {
                drawingFirstPersonSkin = previous;
            }
        }
        return true;
    }

    private static Point viewPalm(Point mount, Point modelMount, Point contact, Quaternionf rotation) {
        Vector3f offset = contact.offsetFrom(modelMount, rotation, FIRST_SCALE, new Vector3f());
        return new Point(mount.x() + offset.x, mount.y() + offset.y, mount.z() + offset.z);
    }

    private static void drawSkinArm(PlayerRenderer renderer, AbstractClientPlayer player, HumanoidArm arm,
                                    Point target, boolean support, Quaternionf scratch, PoseStack poses,
                                    MultiBufferSource buffers, int light) {
        float sign = HeldGripPose.side(arm);
        // PlayerRenderer renders a reset arm and sleeve. At age zero vanilla arm bob is ±0.1 Z.
        float bob = sign * 0.1F;
        // The wood may overlap the last block of a stock arm; this reads as a closed grip without
        // a separate hand, a bent player mesh, or trying to balance open ends underneath the drill.
        boolean leftAuxiliary = support && arm == HumanoidArm.LEFT;
        float palmX = sign * (support ? 0.52F : 0.65F);
        float palmY = leftAuxiliary ? 9.55F : support ? 9.15F : 9.10F;
        float palmZ = leftAuxiliary ? 0.98F : support ? 1.55F : -1.10F;
        Point palm = new Point((-sign * 5 + palmX * Mth.cos(bob) - palmY * Mth.sin(bob)) / 16,
                (2 + palmX * Mth.sin(bob) + palmY * Mth.cos(bob)) / 16, palmZ / 16);
        // The left arm approaches the bar from the outer/rear side. Roll the entire stock cuboid
        // so its distal edge crosses the wood broadly instead of pointing one corner at it.
        Point shoulder = leftAuxiliary ? new Point(-0.18F, -0.76F, -0.25F)
                : new Point(sign * (support ? 0.52F : 0.64F),
                        support ? -0.74F : -0.76F, support ? -0.28F : -0.24F);
        float roll = leftAuxiliary ? -0.30F : sign * (support ? -0.30F : 0.05F);
        poses.pushPose();
        HeldGripPose.placeArm(poses, palm, target, shoulder, roll, scratch);
        if (arm == HumanoidArm.RIGHT) {
            renderer.renderRightHand(poses, buffers, light, player);
        } else {
            renderer.renderLeftHand(poses, buffers, light, player);
        }
        poses.popPose();
    }
}

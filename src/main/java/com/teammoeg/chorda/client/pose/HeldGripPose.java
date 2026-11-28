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

package com.teammoeg.chorda.client.pose;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

/** Stateless, model-independent helpers for keeping a real player hand on a held object. */
public final class HeldGripPose {
    private HeldGripPose() {}

    public record Point(float x, float y, float z) {
        /** Positions this local grip at the current origin of the pose. */
        public void placeAtOrigin(PoseStack poses) {
            poses.translate(-x, -y, -z);
        }

        /** Writes the rotated and scaled difference from a fixed grip without retaining scratch data. */
        public Vector3f offsetFrom(Point fixed, Quaternionfc rotation, float scale, Vector3f dest) {
            return rotation.transform(dest.set((x - fixed.x) * scale, (y - fixed.y) * scale, (z - fixed.z) * scale));
        }
    }

    public static float side(HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? 1 : -1;
    }

    /**
     * Aims the stock arm's local +Y axis from shoulder to target and places its local palm there.
     * Target and shoulder are in the current pose's coordinates; wristRoll is in radians.
     */
    public static void placeArm(PoseStack poses, Point palm, Point target, Point shoulder,
                                float wristRoll, Quaternionf scratchRotation) {
        poses.translate(target.x, target.y, target.z);
        poses.mulPose(scratchRotation.rotationTo(0, 1, 0,
                target.x - shoulder.x, target.y - shoulder.y, target.z - shoulder.z).rotateY(wristRoll));
        palm.placeAtOrigin(poses);
    }

    /** Smoothly dismisses a grip while swapping the displayed stack. */
    public static float equipWeight(float equipProgress) {
        float value = Mth.clamp(equipProgress, 0, 1);
        return value * value * (3 - 2 * value);
    }
}

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

package com.teammoeg.frostedheart.content.steamenergy.charger.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.teammoeg.frostedheart.content.steamenergy.charger.ChargerTileEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/** Shows the docked item's stock static model beside the charger nozzle. */
public final class ChargerDockRenderer implements BlockEntityRenderer<ChargerTileEntity> {
    private static final Quaternionf ALONG_TRAY = Axis.YP.rotationDegrees(90);
    private static final Quaternionf[] ORIENTATIONS = new Quaternionf[Direction.values().length];

    static {
        // The same facing transforms as blockstates/charger.json.
        for (Direction direction : Direction.values()) {
            ORIENTATIONS[direction.ordinal()] = (switch (direction) {
                case NORTH -> BlockModelRotation.X0_Y0;
                case EAST -> BlockModelRotation.X0_Y90;
                case SOUTH -> BlockModelRotation.X0_Y180;
                case WEST -> BlockModelRotation.X0_Y270;
                case UP -> BlockModelRotation.X270_Y0;
                case DOWN -> BlockModelRotation.X90_Y0;
            }).getRotation().getLeftRotation();
        }
    }

    private final ItemRenderer items;

    public ChargerDockRenderer(BlockEntityRendererProvider.Context context) {
        items = context.getItemRenderer();
    }

    @Override
    public boolean shouldRender(ChargerTileEntity charger, Vec3 cameraPosition) {
        return !charger.getDockedDisplay().isEmpty() && BlockEntityRenderer.super.shouldRender(charger, cameraPosition);
    }

    @Override
    public void render(ChargerTileEntity charger, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        ItemStack display = charger.getDockedDisplay();
        Level level = charger.getLevel();
        if (display.isEmpty() || level == null) return;

        Direction facing = charger.getDirection();
        Direction exposed = facing == Direction.UP ? Direction.SOUTH : facing == Direction.DOWN ? Direction.NORTH : Direction.UP;
        int exposedLight = LevelRenderer.getLightColor(level, charger.getBlockPos().relative(exposed));
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(ORIENTATIONS[facing.ordinal()]);
        // For a north-facing charger, the rail rests at y=.75, to the side of its central nozzle.
        pose.translate(0.26, 0.375, -0.18);
        pose.mulPose(ALONG_TRAY);
        pose.scale(0.64F, 0.64F, 0.64F);
        items.renderStatic(display, ItemDisplayContext.NONE, exposedLight, overlay, pose, buffers, level,
                (int) charger.getBlockPos().asLong());
        pose.popPose();
    }
}

/*
 * Copyright (c) 2021-2024 TeamMoeg
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

package com.teammoeg.frostedheart.base.block;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IDirectionalTile;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IDirectionalTile.PlacementLimitation;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IPlayerInteraction;
import blusunrize.immersiveengineering.common.util.DirectionUtils;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.AxisDirection;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

import net.minecraft.block.AbstractBlock.Properties;

public class FHGuiBlock extends FHBaseBlock {

    public FHGuiBlock(Properties blockProps) {
        super(blockProps);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        ActionResultType superResult = super.use(state, world, pos, player, hand, hit);
        if (superResult.consumesAction())
            return superResult;
        final Direction side = hit.getDirection();
        final float hitX = (float) hit.getLocation().x - pos.getX();
        final float hitY = (float) hit.getLocation().y - pos.getY();
        final float hitZ = (float) hit.getLocation().z - pos.getZ();
        ItemStack heldItem = player.getItemInHand(hand);
        TileEntity tile = world.getBlockEntity(pos);
        if (tile instanceof IDirectionalTile && Utils.isHammer(heldItem) && ((IDirectionalTile) tile).canHammerRotate(
                side,
                hit.getLocation().subtract(Vector3d.atLowerCornerOf(pos)),
                player) && !world.isClientSide) {
            Direction f = ((IDirectionalTile) tile).getFacing();
            Direction oldF = f;
            PlacementLimitation limit = ((IDirectionalTile) tile).getFacingLimitation();
            switch (limit) {
                case SIDE_CLICKED:
                    f = DirectionUtils.VALUES[Math.floorMod(f.ordinal() + (player.isShiftKeyDown() ? -1 : 1), DirectionUtils.VALUES.length)];
                    break;
                case PISTON_LIKE:
                    f = player.isShiftKeyDown() != (side.getAxisDirection() == AxisDirection.NEGATIVE) ? DirectionUtils.rotateAround(f, side.getAxis()).getOpposite() : DirectionUtils.rotateAround(f, side.getAxis());
                    break;
                case HORIZONTAL:
                case HORIZONTAL_PREFER_SIDE:
                case HORIZONTAL_QUADRANT:
                case HORIZONTAL_AXIS:
                    f = player.isShiftKeyDown() != side.equals(Direction.DOWN) ? f.getCounterClockWise() : f.getClockWise();
                    break;
            }
            ((IDirectionalTile) tile).setFacing(f);
            ((IDirectionalTile) tile).afterRotation(oldF, f);
            tile.setChanged();
            world.sendBlockUpdated(pos, state, state, 3);
            world.blockEvent(tile.getBlockPos(), tile.getBlockState().getBlock(), 255, 0);
            return ActionResultType.SUCCESS;
        }
        if (tile instanceof IPlayerInteraction) {
            boolean b = ((IPlayerInteraction) tile).interact(side, player, hand, heldItem, hitX, hitY, hitZ);
            if (b)
                return ActionResultType.SUCCESS;
        }
        if (tile instanceof IInteractionObjectIE && hand == Hand.MAIN_HAND && !player.isShiftKeyDown()) {
            IInteractionObjectIE interaction = (IInteractionObjectIE) tile;
            interaction = interaction.getGuiMaster();
            if (interaction != null && interaction.canUseGui(player) && !world.isClientSide)
                NetworkHooks.openGui((ServerPlayerEntity) player, interaction, ((TileEntity) interaction).getBlockPos());
            return ActionResultType.SUCCESS;
        }
        return superResult;
    }

}

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

package com.teammoeg.chorda.block;

import blusunrize.immersiveengineering.api.utils.DirectionUtils;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IDirectionalBE;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IPlayerInteraction;
import blusunrize.immersiveengineering.common.blocks.PlacementLimitation;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.level.Level;

/**
 * 具有 GUI 交互功能的方块基类，结合了 {@link CBlock} 的透光度支持和 {@link CEntityBlock} 的方块实体支持。
 * 处理玩家右键交互，支持沉浸工程（IE）的方向旋转、玩家交互接口以及菜单打开。
 * <p>
 * Abstract base block class with GUI interaction capabilities, combining {@link CBlock}'s
 * light opacity support with {@link CEntityBlock}'s block entity support.
 * Handles player right-click interactions including IE directional rotation with hammers,
 * player interaction interfaces, and menu provider opening.
 *
 * @param <T> 方块实体类型 / the block entity type
 */
public abstract class CGuiBlock<T extends BlockEntity> extends CBlock implements CEntityBlock<T> {

    /**
     * 使用给定的方块属性构造 GUI 方块。
     * <p>
     * Constructs a GUI block with the given properties.
     *
     * @param blockProps 方块属性 / the block properties
     */
    public CGuiBlock(Properties blockProps) {
        super(blockProps);
    }

    /**
     * 处理玩家对方块的右键交互。依次检查：
     * <ol>
     *   <li>父类交互结果</li>
     *   <li>使用锤子旋转方向性方块实体（IE 集成）</li>
     *   <li>玩家交互接口（IPlayerInteraction）</li>
     *   <li>IE 交互对象或菜单提供者以打开 GUI</li>
     * </ol>
     * <p>
     * Handles player right-click interactions with this block. Checks in order:
     * <ol>
     *   <li>Super class interaction result</li>
     *   <li>Hammer rotation for directional block entities (IE integration)</li>
     *   <li>Player interaction interface (IPlayerInteraction)</li>
     *   <li>IE interaction object or menu provider to open GUI</li>
     * </ol>
     *
     * @param state 方块状态 / the block state
     * @param world 当前世界 / the level
     * @param pos 方块位置 / the block position
     * @param player 交互的玩家 / the interacting player
     * @param hand 使用的手 / the hand used
     * @param hit 方块命中结果 / the block hit result
     * @return 交互结果 / the interaction result
     */
    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        InteractionResult superResult = super.use(state, world, pos, player, hand, hit);
        if (superResult.consumesAction())
            return superResult;
        final Direction side = hit.getDirection();
        final float hitX = (float) hit.getLocation().x - pos.getX();
        final float hitY = (float) hit.getLocation().y - pos.getY();
        final float hitZ = (float) hit.getLocation().z - pos.getZ();
        ItemStack heldItem = player.getItemInHand(hand);
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof IDirectionalBE && Utils.isHammer(heldItem) && ((IDirectionalBE) tile).canHammerRotate(
                side,
                hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)),
                player) && !world.isClientSide) {
            Direction f = ((IDirectionalBE) tile).getFacing();
            Direction oldF = f;
            PlacementLimitation limit = ((IDirectionalBE) tile).getFacingLimitation();
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
            ((IDirectionalBE) tile).setFacing(f);
            ((IDirectionalBE) tile).afterRotation(oldF, f);
            tile.setChanged();
            world.sendBlockUpdated(pos, state, state, 3);
            world.blockEvent(tile.getBlockPos(), tile.getBlockState().getBlock(), 255, 0);
            return InteractionResult.SUCCESS;
        }
        if (tile instanceof IPlayerInteraction) {
            boolean b = ((IPlayerInteraction) tile).interact(side, player, hand, heldItem, hitX, hitY, hitZ);
            if (b)
                return InteractionResult.SUCCESS;
        }
        if(hand == InteractionHand.MAIN_HAND && !player.isShiftKeyDown()) {
	        if (tile instanceof IInteractionObjectIE interaction) {
	            BlockEntity master = interaction.getGuiMaster();
	            if (!world.isClientSide&&master instanceof MenuProvider menu)
	            	NetworkHooks.openScreen((ServerPlayer)player, menu,master.getBlockPos());
	            return InteractionResult.SUCCESS;
	        }
	        if (tile instanceof MenuProvider menu) {
	        	if (!world.isClientSide)
	        		NetworkHooks.openScreen((ServerPlayer)player, menu,tile.getBlockPos());
	            return InteractionResult.SUCCESS;
	        }
        }
        return superResult;
    }

}

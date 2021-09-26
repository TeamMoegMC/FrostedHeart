package com.teammoeg.frostedheart.block;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IDirectionalTile;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IDirectionalTile.PlacementLimitation;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IInteractionObjectIE;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IPlayerInteraction;
import blusunrize.immersiveengineering.common.util.DirectionUtils;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
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

import java.util.function.BiFunction;

public class FHGuiBlock extends FHBaseBlock {

    public FHGuiBlock(String name, Properties blockProps,
                      BiFunction<Block, net.minecraft.item.Item.Properties, Item> createItemBlock) {
        super(name, blockProps, createItemBlock);
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        ActionResultType superResult = super.onBlockActivated(state, world, pos, player, hand, hit);
        if (superResult.isSuccessOrConsume())
            return superResult;
        final Direction side = hit.getFace();
        final float hitX = (float) hit.getHitVec().x - pos.getX();
        final float hitY = (float) hit.getHitVec().y - pos.getY();
        final float hitZ = (float) hit.getHitVec().z - pos.getZ();
        ItemStack heldItem = player.getHeldItem(hand);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof IDirectionalTile && Utils.isHammer(heldItem) && ((IDirectionalTile) tile).canHammerRotate(
                side,
                hit.getHitVec().subtract(Vector3d.copy(pos)),
                player) && !world.isRemote) {
            Direction f = ((IDirectionalTile) tile).getFacing();
            Direction oldF = f;
            PlacementLimitation limit = ((IDirectionalTile) tile).getFacingLimitation();
            switch (limit) {
                case SIDE_CLICKED:
                    f = DirectionUtils.VALUES[Math.floorMod(f.ordinal() + (player.isSneaking() ? -1 : 1), DirectionUtils.VALUES.length)];
                    break;
                case PISTON_LIKE:
                    f = player.isSneaking() != (side.getAxisDirection() == AxisDirection.NEGATIVE) ? DirectionUtils.rotateAround(f, side.getAxis()).getOpposite() : DirectionUtils.rotateAround(f, side.getAxis());
                    break;
                case HORIZONTAL:
                case HORIZONTAL_PREFER_SIDE:
                case HORIZONTAL_QUADRANT:
                case HORIZONTAL_AXIS:
                    f = player.isSneaking() != side.equals(Direction.DOWN) ? f.rotateYCCW() : f.rotateY();
                    break;
            }
            ((IDirectionalTile) tile).setFacing(f);
            ((IDirectionalTile) tile).afterRotation(oldF, f);
            tile.markDirty();
            world.notifyBlockUpdate(pos, state, state, 3);
            world.addBlockEvent(tile.getPos(), tile.getBlockState().getBlock(), 255, 0);
            return ActionResultType.SUCCESS;
        }
        if (tile instanceof IPlayerInteraction) {
            boolean b = ((IPlayerInteraction) tile).interact(side, player, hand, heldItem, hitX, hitY, hitZ);
            if (b)
                return ActionResultType.SUCCESS;
        }
        if (tile instanceof IInteractionObjectIE && hand == Hand.MAIN_HAND && !player.isSneaking()) {
            IInteractionObjectIE interaction = (IInteractionObjectIE) tile;
            interaction = interaction.getGuiMaster();
            if (interaction != null && interaction.canUseGui(player) && !world.isRemote)
                NetworkHooks.openGui((ServerPlayerEntity) player, interaction, ((TileEntity) interaction).getPos());
            return ActionResultType.SUCCESS;
        }
        return superResult;
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

}

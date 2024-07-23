/*
 * Copyright (c) 2022-2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.utility;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.teammoeg.frostedheart.base.item.FHBaseItem;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IBucketPickupHandler;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import com.teammoeg.frostedheart.base.capability.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStackSimple;

import net.minecraft.item.Item.Properties;

public class CeramicBucket extends FHBaseItem {
    public CeramicBucket(Properties properties) {
        super(properties);
    }


    private boolean canBlockContainFluid(World worldIn, BlockPos posIn, BlockState blockstate, Fluid fluid) {
        return blockstate.getBlock() instanceof ILiquidContainer && ((ILiquidContainer) blockstate.getBlock()).canPlaceLiquid(worldIn, posIn, blockstate, fluid);
    }

    private ItemStack emptyBucket(ItemStack stack, PlayerEntity playerIn) {
        if (playerIn.abilities.instabuild) {
            return stack;
        }
        ItemStack emptyStack = stack.copy();
        emptyStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE));
        return emptyStack;
    }

    private ItemStack fillBucket(ItemStack emptyBuckets, PlayerEntity player, Fluid fillFluid) {
        if (player.abilities.instabuild) {
            return emptyBuckets;
        }
        ItemStack filledStack = emptyBuckets.split(1);
        filledStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> handler.fill(new FluidStack(fillFluid, 1000), IFluidHandler.FluidAction.EXECUTE));
        if (emptyBuckets.isEmpty()) {
            return filledStack;
        }
        if (!player.inventory.add(filledStack)) {
            player.drop(filledStack, false);
        }
        return emptyBuckets;
    }

    private Fluid getFluid(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> handler.getFluidInTank(0).getFluid()).orElse(Fluids.EMPTY);
    }

    @Override
    public int getItemStackLimit(ItemStack stack) {
        return this.getFluid(stack) == Fluids.EMPTY ? 16 : 1;
    }

    @Override
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, @Nullable net.minecraft.nbt.CompoundNBT nbt) {
        return new FluidHandlerItemStackSimple(stack, 1000) {
            @Override
            public boolean canFillFluidType(FluidStack fluid) {
                return isFluidValid(0, fluid);
            }

            @Override
            public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
                return !stack.getFluid().getAttributes().isGaseous();
            }

        };
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);
        Fluid containedFluid = getFluid(itemstack);
        BlockRayTraceResult raytraceresult = getPlayerPOVHitResult(worldIn, playerIn, containedFluid == Fluids.EMPTY ? RayTraceContext.FluidMode.SOURCE_ONLY : RayTraceContext.FluidMode.NONE);
        ActionResult<ItemStack> ret = net.minecraftforge.event.ForgeEventFactory.onBucketUse(playerIn, worldIn, itemstack, raytraceresult);

        if (ret != null) return ret;
        if (raytraceresult.getType() == RayTraceResult.Type.MISS) {
            return ActionResult.pass(itemstack);
        } else if (raytraceresult.getType() != RayTraceResult.Type.BLOCK) {
            return ActionResult.pass(itemstack);
        } else {
            BlockPos blockpos = raytraceresult.getBlockPos();
            Direction direction = raytraceresult.getDirection();
            BlockPos blockpos1 = blockpos.relative(direction);
            if (worldIn.mayInteract(playerIn, blockpos) && playerIn.mayUseItemAt(blockpos1, direction, itemstack)) {
                if (containedFluid == Fluids.EMPTY) {
                    BlockState blockstate1 = worldIn.getBlockState(blockpos);
                    if (blockstate1.getBlock() instanceof IBucketPickupHandler) {
                        Fluid fluid1 = ((IBucketPickupHandler) blockstate1.getBlock()).takeLiquid(worldIn, blockpos, blockstate1);
                        if (fluid1 != Fluids.EMPTY) {
                            SoundEvent soundevent = containedFluid.getAttributes().getFillSound();
                            if (soundevent == null)
                                soundevent = fluid1.is(FluidTags.LAVA) ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL;
                            playerIn.playSound(soundevent, 1.0F, 1.0F);
                            ItemStack itemstack1 = this.fillBucket(itemstack, playerIn, fluid1);
                            if (!worldIn.isClientSide) {
                                CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayerEntity) playerIn, new ItemStack(fluid1.getBucket()));
                            }

                            return ActionResult.sidedSuccess(itemstack1, worldIn.isClientSide());
                        }
                    }

                    return ActionResult.fail(itemstack);
                }
                if (itemstack.getCount() > 1)
                    return ActionResult.fail(itemstack);
                BlockState blockstate = worldIn.getBlockState(blockpos);
                BlockPos blockpos2 = canBlockContainFluid(worldIn, blockpos, blockstate, containedFluid) ? blockpos : blockpos1;
                if (this.tryPlaceContainedLiquid(playerIn, worldIn, blockpos2, raytraceresult, containedFluid)) {
                    if (playerIn instanceof ServerPlayerEntity) {
                        CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayerEntity) playerIn, blockpos2, itemstack);
                    }
                    return ActionResult.sidedSuccess(this.emptyBucket(itemstack, playerIn), worldIn.isClientSide());
                }
            }
            return ActionResult.fail(itemstack);
        }
    }

    protected void playEmptySound(@Nullable PlayerEntity player, IWorld worldIn, BlockPos pos, Fluid fluid) {
        SoundEvent soundevent = fluid.getAttributes().getEmptySound();
        if (soundevent == null)
            soundevent = fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
        worldIn.playSound(player, pos, soundevent, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    public boolean tryPlaceContainedLiquid(@Nullable PlayerEntity player, World worldIn, BlockPos posIn, @Nullable BlockRayTraceResult rayTrace, Fluid fluid) {
        if (!(fluid instanceof FlowingFluid)) {
            return false;
        }
        BlockState blockstate = worldIn.getBlockState(posIn);
        Block block = blockstate.getBlock();
        Material material = blockstate.getMaterial();
        boolean flag = blockstate.canBeReplaced(fluid);
        boolean flag1 = blockstate.isAir() || flag || block instanceof ILiquidContainer && ((ILiquidContainer) block).canPlaceLiquid(worldIn, posIn, blockstate, fluid);
        if (!flag1) {
            return rayTrace != null && this.tryPlaceContainedLiquid(player, worldIn, rayTrace.getBlockPos().relative(rayTrace.getDirection()), null, fluid);
        } else if (worldIn.dimensionType().ultraWarm() && fluid.is(FluidTags.WATER)) {
            int i = posIn.getX();
            int j = posIn.getY();
            int k = posIn.getZ();
            worldIn.playSound(player, posIn, SoundEvents.FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (worldIn.random.nextFloat() - worldIn.random.nextFloat()) * 0.8F);

            for (int l = 0; l < 8; ++l) {
                worldIn.addParticle(ParticleTypes.LARGE_SMOKE, (double) i + Math.random(), (double) j + Math.random(), (double) k + Math.random(), 0.0D, 0.0D, 0.0D);
            }

            return true;
        } else if (block instanceof ILiquidContainer && ((ILiquidContainer) block).canPlaceLiquid(worldIn, posIn, blockstate, fluid)) {
            ((ILiquidContainer) block).placeLiquid(worldIn, posIn, blockstate, ((FlowingFluid) fluid).getSource(false));
            this.playEmptySound(player, worldIn, posIn, fluid);
            return true;
        } else {
            if (!worldIn.isClientSide && flag && !material.isLiquid()) {
                worldIn.destroyBlock(posIn, true);
            }

            if (!worldIn.setBlock(posIn, fluid.defaultFluidState().createLegacyBlock(), 11) && !blockstate.getFluidState().isSource()) {
                return false;
            }
            this.playEmptySound(player, worldIn, posIn, fluid);
            return true;
        }
    }
}

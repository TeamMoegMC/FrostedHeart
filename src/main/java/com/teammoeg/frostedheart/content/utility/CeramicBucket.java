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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import com.teammoeg.frostedheart.base.capability.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStackSimple;

import net.minecraft.world.item.Item.Properties;

public class CeramicBucket extends FHBaseItem {
    public CeramicBucket(Properties properties) {
        super(properties);
    }


    private boolean canBlockContainFluid(Level worldIn, BlockPos posIn, BlockState blockstate, Fluid fluid) {
        return blockstate.getBlock() instanceof LiquidBlockContainer && ((LiquidBlockContainer) blockstate.getBlock()).canPlaceLiquid(worldIn, posIn, blockstate, fluid);
    }

    private ItemStack emptyBucket(ItemStack stack, Player playerIn) {
        if (playerIn.abilities.instabuild) {
            return stack;
        }
        ItemStack emptyStack = stack.copy();
        emptyStack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).ifPresent(handler -> handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE));
        return emptyStack;
    }

    private ItemStack fillBucket(ItemStack emptyBuckets, Player player, Fluid fillFluid) {
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
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, @Nullable net.minecraft.nbt.CompoundTag nbt) {
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
    public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack itemstack = playerIn.getItemInHand(handIn);
        Fluid containedFluid = getFluid(itemstack);
        BlockHitResult raytraceresult = getPlayerPOVHitResult(worldIn, playerIn, containedFluid == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE);
        InteractionResultHolder<ItemStack> ret = net.minecraftforge.event.ForgeEventFactory.onBucketUse(playerIn, worldIn, itemstack, raytraceresult);

        if (ret != null) return ret;
        if (raytraceresult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(itemstack);
        } else if (raytraceresult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemstack);
        } else {
            BlockPos blockpos = raytraceresult.getBlockPos();
            Direction direction = raytraceresult.getDirection();
            BlockPos blockpos1 = blockpos.relative(direction);
            if (worldIn.mayInteract(playerIn, blockpos) && playerIn.mayUseItemAt(blockpos1, direction, itemstack)) {
                if (containedFluid == Fluids.EMPTY) {
                    BlockState blockstate1 = worldIn.getBlockState(blockpos);
                    if (blockstate1.getBlock() instanceof BucketPickup) {
                        Fluid fluid1 = ((BucketPickup) blockstate1.getBlock()).takeLiquid(worldIn, blockpos, blockstate1);
                        if (fluid1 != Fluids.EMPTY) {
                            SoundEvent soundevent = containedFluid.getAttributes().getFillSound();
                            if (soundevent == null)
                                soundevent = fluid1.is(FluidTags.LAVA) ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL;
                            playerIn.playSound(soundevent, 1.0F, 1.0F);
                            ItemStack itemstack1 = this.fillBucket(itemstack, playerIn, fluid1);
                            if (!worldIn.isClientSide) {
                                CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer) playerIn, new ItemStack(fluid1.getBucket()));
                            }

                            return InteractionResultHolder.sidedSuccess(itemstack1, worldIn.isClientSide());
                        }
                    }

                    return InteractionResultHolder.fail(itemstack);
                }
                if (itemstack.getCount() > 1)
                    return InteractionResultHolder.fail(itemstack);
                BlockState blockstate = worldIn.getBlockState(blockpos);
                BlockPos blockpos2 = canBlockContainFluid(worldIn, blockpos, blockstate, containedFluid) ? blockpos : blockpos1;
                if (this.tryPlaceContainedLiquid(playerIn, worldIn, blockpos2, raytraceresult, containedFluid)) {
                    if (playerIn instanceof ServerPlayer) {
                        CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) playerIn, blockpos2, itemstack);
                    }
                    return InteractionResultHolder.sidedSuccess(this.emptyBucket(itemstack, playerIn), worldIn.isClientSide());
                }
            }
            return InteractionResultHolder.fail(itemstack);
        }
    }

    protected void playEmptySound(@Nullable Player player, LevelAccessor worldIn, BlockPos pos, Fluid fluid) {
        SoundEvent soundevent = fluid.getAttributes().getEmptySound();
        if (soundevent == null)
            soundevent = fluid.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
        worldIn.playSound(player, pos, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public boolean tryPlaceContainedLiquid(@Nullable Player player, Level worldIn, BlockPos posIn, @Nullable BlockHitResult rayTrace, Fluid fluid) {
        if (!(fluid instanceof FlowingFluid)) {
            return false;
        }
        BlockState blockstate = worldIn.getBlockState(posIn);
        Block block = blockstate.getBlock();
        Material material = blockstate.getMaterial();
        boolean flag = blockstate.canBeReplaced(fluid);
        boolean flag1 = blockstate.isAir() || flag || block instanceof LiquidBlockContainer && ((LiquidBlockContainer) block).canPlaceLiquid(worldIn, posIn, blockstate, fluid);
        if (!flag1) {
            return rayTrace != null && this.tryPlaceContainedLiquid(player, worldIn, rayTrace.getBlockPos().relative(rayTrace.getDirection()), null, fluid);
        } else if (worldIn.dimensionType().ultraWarm() && fluid.is(FluidTags.WATER)) {
            int i = posIn.getX();
            int j = posIn.getY();
            int k = posIn.getZ();
            worldIn.playSound(player, posIn, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (worldIn.random.nextFloat() - worldIn.random.nextFloat()) * 0.8F);

            for (int l = 0; l < 8; ++l) {
                worldIn.addParticle(ParticleTypes.LARGE_SMOKE, (double) i + Math.random(), (double) j + Math.random(), (double) k + Math.random(), 0.0D, 0.0D, 0.0D);
            }

            return true;
        } else if (block instanceof LiquidBlockContainer && ((LiquidBlockContainer) block).canPlaceLiquid(worldIn, posIn, blockstate, fluid)) {
            ((LiquidBlockContainer) block).placeLiquid(worldIn, posIn, blockstate, ((FlowingFluid) fluid).getSource(false));
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

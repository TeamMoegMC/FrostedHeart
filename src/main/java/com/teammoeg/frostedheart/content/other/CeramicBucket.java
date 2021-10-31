package com.teammoeg.frostedheart.content.other;

import com.teammoeg.frostedheart.base.item.FHBaseItem;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IBucketPickupHandler;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import javax.annotation.Nullable;

public class CeramicBucket extends FHBaseItem {
    public CeramicBucket(String name, Properties properties) {
        super(name, properties);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        Fluid fluid = getFluid(itemstack);
        RayTraceResult raytraceresult = rayTrace(worldIn, playerIn, fluid == Fluids.EMPTY ? RayTraceContext.FluidMode.SOURCE_ONLY : RayTraceContext.FluidMode.NONE);
        ActionResult<ItemStack> ret = net.minecraftforge.event.ForgeEventFactory.onBucketUse(playerIn, worldIn, itemstack, raytraceresult);
        if (ret != null) return ret;
        if (raytraceresult.getType() == RayTraceResult.Type.MISS) {
            return ActionResult.resultPass(itemstack);
        } else if (raytraceresult.getType() != RayTraceResult.Type.BLOCK) {
            return ActionResult.resultPass(itemstack);
        } else {
            BlockRayTraceResult blockraytraceresult = (BlockRayTraceResult) raytraceresult;
            BlockPos blockpos = blockraytraceresult.getPos();
            Direction direction = blockraytraceresult.getFace();
            BlockPos blockpos1 = blockpos.offset(direction);
            if (worldIn.isBlockModifiable(playerIn, blockpos) && playerIn.canPlayerEdit(blockpos1, direction, itemstack)) {
                if (fluid == Fluids.EMPTY) {
                    BlockState blockstate1 = worldIn.getBlockState(blockpos);
                    if (blockstate1.getBlock() instanceof IBucketPickupHandler) {
                        Fluid fluid1 = ((IBucketPickupHandler) blockstate1.getBlock()).pickupFluid(worldIn, blockpos, blockstate1);
                        if (fluid1 != Fluids.EMPTY) {
                            playerIn.addStat(Stats.ITEM_USED.get(this));

                            SoundEvent soundevent = fluid.getAttributes().getFillSound();
                            if (soundevent == null)
                                soundevent = fluid.isIn(FluidTags.LAVA) ? SoundEvents.ITEM_BUCKET_FILL_LAVA : SoundEvents.ITEM_BUCKET_FILL;
                            playerIn.playSound(soundevent, 1.0F, 1.0F);
                            ItemStack itemstack1 = DrinkHelper.fill(itemstack, playerIn, new ItemStack(fluid.getFilledBucket()));
                            if (!worldIn.isRemote) {
                                CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayerEntity) playerIn, new ItemStack(fluid.getFilledBucket()));
                            }

                            return ActionResult.func_233538_a_(itemstack1, worldIn.isRemote());
                        }
                    }

                    return ActionResult.resultFail(itemstack);
                } else {
                    BlockState blockstate = worldIn.getBlockState(blockpos);
                    BlockPos blockpos2 = canBlockContainFluid(worldIn, blockpos, blockstate, fluid) ? blockpos : blockpos1;
                    Block block = worldIn.getBlockState(blockpos2).getBlock();
                    if (fluid instanceof FlowingFluid && blockstate.isAir() || blockstate.isReplaceable(fluid)
                            && block instanceof ILiquidContainer && ((ILiquidContainer) block).canContainFluid(worldIn, blockpos2, blockstate, fluid)) {

                        return ActionResult.func_233538_a_(this.emptyBucket(itemstack, playerIn), worldIn.isRemote());
                    } else if (worldIn.getDimensionType().isUltrawarm() && fluid.isIn(FluidTags.WATER)) {

                        worldIn.playSound(playerIn, blockpos2, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (worldIn.rand.nextFloat() - worldIn.rand.nextFloat()) * 0.8F);

                        for (int l = 0; l < 8; ++l) {
                            worldIn.addParticle(ParticleTypes.LARGE_SMOKE, blockpos2.getX() + Math.random(), blockpos2.getY() + Math.random(), blockpos2.getZ() + Math.random(), 0.0D, 0.0D, 0.0D);
                        }
                        return ActionResult.func_233538_a_(this.emptyBucket(itemstack, playerIn), worldIn.isRemote());
                    } else if (block instanceof ILiquidContainer && ((ILiquidContainer) block).canContainFluid(worldIn, blockpos2, blockstate, fluid)) {
                        ((ILiquidContainer) block).receiveFluid(worldIn, blockpos2, blockstate, ((FlowingFluid) fluid).getStillFluidState(false));
                        this.playEmptySound(playerIn, worldIn, blockpos2, fluid);
                        return ActionResult.func_233538_a_(this.emptyBucket(itemstack, playerIn), worldIn.isRemote());
                    } else {
                        if (!worldIn.isRemote && blockstate.isReplaceable(fluid) && !blockstate.getMaterial().isLiquid()) {
                            worldIn.destroyBlock(blockpos2, true);
                        }

                        if (!worldIn.setBlockState(blockpos2, fluid.getDefaultState().getBlockState(), 11) && !blockstate.getFluidState().isSource()) {
                            ActionResult.resultFail(itemstack);
                        } else {
                            this.playEmptySound(playerIn, worldIn, blockpos2, fluid);
                            return ActionResult.func_233538_a_(this.emptyBucket(itemstack, playerIn), worldIn.isRemote());
                        }
                    }
                }
            }
        }
        return ActionResult.resultFail(itemstack);
    }

    protected ItemStack emptyBucket(ItemStack stack, PlayerEntity player) {
        return !player.abilities.isCreativeMode ? new ItemStack(Items.BUCKET) : stack;
    }

    protected void playEmptySound(@Nullable PlayerEntity player, IWorld worldIn, BlockPos pos, Fluid fluid) {
        SoundEvent soundevent = fluid.getAttributes().getEmptySound();
        if (soundevent == null)
            soundevent = fluid.isIn(FluidTags.LAVA) ? SoundEvents.ITEM_BUCKET_EMPTY_LAVA : SoundEvents.ITEM_BUCKET_EMPTY;
        worldIn.playSound(player, pos, soundevent, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, @Nullable net.minecraft.nbt.CompoundNBT nbt) {
        if (this.getClass() == CeramicBucket.class)
            return new net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper(stack);
        else
            return super.initCapabilities(stack, nbt);
    }

    public Fluid getFluid(ItemStack stack) {
        return stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).map(handler -> handler.getFluidInTank(0).getFluid()).orElse(Fluids.EMPTY);
    }

    private boolean canBlockContainFluid(World worldIn, BlockPos posIn, BlockState blockstate, Fluid fluid) {
        return blockstate.getBlock() instanceof ILiquidContainer && ((ILiquidContainer) blockstate.getBlock()).canContainFluid(worldIn, posIn, blockstate, fluid);
    }
}

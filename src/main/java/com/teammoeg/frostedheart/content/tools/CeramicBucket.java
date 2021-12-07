package com.teammoeg.frostedheart.content.tools;

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
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStackSimple;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CeramicBucket extends FHBaseItem {
    public CeramicBucket(String name, Properties properties) {
        super(name, properties);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemstack = playerIn.getHeldItem(handIn);
        Fluid containedFluid = getFluid(itemstack);
        RayTraceResult raytraceresult = rayTrace(worldIn, playerIn, containedFluid == Fluids.EMPTY ? RayTraceContext.FluidMode.SOURCE_ONLY : RayTraceContext.FluidMode.NONE);
        ActionResult<ItemStack> ret = net.minecraftforge.event.ForgeEventFactory.onBucketUse(playerIn, worldIn, itemstack, raytraceresult);
        if (ret != null)return ret;
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
                if (containedFluid == Fluids.EMPTY) {
                    BlockState blockstate1 = worldIn.getBlockState(blockpos);
                    if (blockstate1.getBlock() instanceof IBucketPickupHandler) {
                        Fluid fluid1 = ((IBucketPickupHandler) blockstate1.getBlock()).pickupFluid(worldIn, blockpos, blockstate1);
                        if (fluid1 != Fluids.EMPTY) {


                            SoundEvent soundevent = containedFluid.getAttributes().getFillSound();
                            if (soundevent == null)
                                soundevent = fluid1.isIn(FluidTags.LAVA) ? SoundEvents.ITEM_BUCKET_FILL_LAVA : SoundEvents.ITEM_BUCKET_FILL;
                            playerIn.playSound(soundevent, 1.0F, 1.0F);
                            ItemStack itemstack1 = this.fillBucket(itemstack, playerIn, fluid1);
                            if (!worldIn.isRemote) {
                                CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayerEntity) playerIn, new ItemStack(fluid1.getFilledBucket()));
                            }

                            return ActionResult.func_233538_a_(itemstack1, worldIn.isRemote());
                        }
                    }

                    return ActionResult.resultFail(itemstack);
                }
				BlockState blockstate = worldIn.getBlockState(blockpos);
				BlockPos blockpos2 = canBlockContainFluid(worldIn, blockpos, blockstate, containedFluid) ? blockpos : blockpos1;
				if (this.tryPlaceContainedLiquid(playerIn, worldIn, blockpos2, blockraytraceresult, containedFluid)) {
				    if (playerIn instanceof ServerPlayerEntity) {
				        CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayerEntity) playerIn, blockpos2, itemstack);
				    }
				    return ActionResult.func_233538_a_(this.emptyBucket(itemstack, playerIn), worldIn.isRemote());
				}
            }
			return ActionResult.resultFail(itemstack);
        }
    }

    public boolean tryPlaceContainedLiquid(@Nullable PlayerEntity player, World worldIn, BlockPos posIn, @Nullable BlockRayTraceResult rayTrace, Fluid fluid) {
        if (!(fluid instanceof FlowingFluid)) {
            return false;
        }
		BlockState blockstate = worldIn.getBlockState(posIn);
		Block block = blockstate.getBlock();
		Material material = blockstate.getMaterial();
		boolean flag = blockstate.isReplaceable(fluid);
		boolean flag1 = blockstate.isAir() || flag || block instanceof ILiquidContainer && ((ILiquidContainer) block).canContainFluid(worldIn, posIn, blockstate, fluid);
		if (!flag1) {
		    return rayTrace != null && this.tryPlaceContainedLiquid(player, worldIn, rayTrace.getPos().offset(rayTrace.getFace()), null, fluid);
		} else if (worldIn.getDimensionType().isUltrawarm() && fluid.isIn(FluidTags.WATER)) {
		    int i = posIn.getX();
		    int j = posIn.getY();
		    int k = posIn.getZ();
		    worldIn.playSound(player, posIn, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 0.5F, 2.6F + (worldIn.rand.nextFloat() - worldIn.rand.nextFloat()) * 0.8F);

		    for (int l = 0; l < 8; ++l) {
		        worldIn.addParticle(ParticleTypes.LARGE_SMOKE, (double) i + Math.random(), (double) j + Math.random(), (double) k + Math.random(), 0.0D, 0.0D, 0.0D);
		    }

		    return true;
		} else if (block instanceof ILiquidContainer && ((ILiquidContainer) block).canContainFluid(worldIn, posIn, blockstate, fluid)) {
		    ((ILiquidContainer) block).receiveFluid(worldIn, posIn, blockstate, ((FlowingFluid) fluid).getStillFluidState(false));
		    this.playEmptySound(player, worldIn, posIn, fluid);
		    return true;
		} else {
		    if (!worldIn.isRemote && flag && !material.isLiquid()) {
		        worldIn.destroyBlock(posIn, true);
		    }

		    if (!worldIn.setBlockState(posIn, fluid.getDefaultState().getBlockState(), 11) && !blockstate.getFluidState().isSource()) {
		        return false;
		    }
			this.playEmptySound(player, worldIn, posIn, fluid);
			return true;
		}
    }

    private ItemStack emptyBucket(ItemStack stack, PlayerEntity playerIn) {
        if (playerIn.abilities.isCreativeMode) {
            return stack;
        }
        ItemStack emptyStack = stack.copy();
        emptyStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).ifPresent(handler -> handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE));
        return emptyStack;
    }

    protected void playEmptySound(@Nullable PlayerEntity player, IWorld worldIn, BlockPos pos, Fluid fluid) {
        SoundEvent soundevent = fluid.getAttributes().getEmptySound();
        if (soundevent == null)
            soundevent = fluid.isIn(FluidTags.LAVA) ? SoundEvents.ITEM_BUCKET_EMPTY_LAVA : SoundEvents.ITEM_BUCKET_EMPTY;
        worldIn.playSound(player, pos, soundevent, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    private ItemStack fillBucket(ItemStack emptyBuckets, PlayerEntity player, Fluid fillFluid) {
        if (player.abilities.isCreativeMode) {
            return emptyBuckets;
        }
		ItemStack filledStack = emptyBuckets.split(1);
		filledStack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).ifPresent(handler -> handler.fill(new FluidStack(fillFluid, 1000), IFluidHandler.FluidAction.EXECUTE));
		if (emptyBuckets.isEmpty()) {
		    return filledStack;
		}
		if (!player.inventory.addItemStackToInventory(filledStack)) {
		    player.dropItem(filledStack, false);
		}
		return emptyBuckets;
    }

    @Override
    public net.minecraftforge.common.capabilities.ICapabilityProvider initCapabilities(ItemStack stack, @Nullable net.minecraft.nbt.CompoundNBT nbt) {
        return new FluidHandlerItemStackSimple(stack, 1000) {
            @Override
            public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {
            	if(stack.getFluid().getAttributes().isGaseous())return false;
                return true;
            }
			@Override
			public boolean canFillFluidType(FluidStack fluid) {
				return isFluidValid(0,fluid);
			}
            
        };
    }

    private Fluid getFluid(ItemStack stack) {
        return stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).map(handler -> handler.getFluidInTank(0).getFluid()).orElse(Fluids.EMPTY);
    }

    private boolean canBlockContainFluid(World worldIn, BlockPos posIn, BlockState blockstate, Fluid fluid) {
        return blockstate.getBlock() instanceof ILiquidContainer && ((ILiquidContainer) blockstate.getBlock()).canContainFluid(worldIn, posIn, blockstate, fluid);
    }
}

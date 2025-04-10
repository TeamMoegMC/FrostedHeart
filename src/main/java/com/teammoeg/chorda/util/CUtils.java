/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.chorda.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.InputConstants;
import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.capability.types.nbt.NBTCapabilityType;
import com.teammoeg.chorda.client.ClientUtils;
import com.teammoeg.chorda.io.NBTSerializable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.wrappers.BucketPickupHandlerWrapper;
import net.minecraftforge.fluids.capability.wrappers.FluidBlockWrapper;
import net.minecraftforge.registries.RegistryObject;

public class CUtils {
	public static final Comparator<ResourceLocation> RESOURCE_LOCATION_COMPARATOR = Comparator.comparing(ResourceLocation::getNamespace).thenComparing(ResourceLocation::getPath);

	public static void applyEffectTo(MobEffectInstance effectinstance, Player playerentity) {
		if (effectinstance.getEffect().isInstantenous()) {
			effectinstance.getEffect().applyInstantenousEffect(playerentity, playerentity, playerentity, effectinstance.getAmplifier(), 1.0D);
		} else {
			playerentity.addEffect(new MobEffectInstance(effectinstance));
		}
	}

	public static Direction dirBetween(BlockPos from, BlockPos to) {
		BlockPos delt = from.subtract(to);
		return Direction.fromDelta(Mth.clamp(delt.getX(), -1, 1), Mth.clamp(delt.getY(), -1, 1), Mth.clamp(delt.getZ(), -1, 1));
	}

	public static BlockEntity getExistingTileEntity(Level world, BlockPos pos) {
		if (world == null)
			return null;
		if (world.hasChunkAt(pos))
			return world.getBlockEntity(pos);
		return null;
	}

	public static BlockEntity getExistingTileEntity(LevelAccessor w, BlockPos pos) {
		if (w == null)
			return null;
		BlockEntity te = null;
		if (w instanceof Level) {
			te = getExistingTileEntity((Level) w, pos);
		} else {
			if (w.hasChunkAt(pos))
				te = w.getBlockEntity(pos);
		}
		return te;
	}

	public static <T> T getExistingTileEntity(LevelAccessor w, BlockPos pos, Class<T> type) {
		BlockEntity te = getExistingTileEntity(w, pos);
		if (type.isInstance(te))
			return (T) te;
		return null;
	}

	public static <T> T getCapability(LevelAccessor w, BlockPos pos, Direction d, Capability<T> cap) {
		BlockEntity te = getExistingTileEntity(w, pos);
		if (te != null)
			return te.getCapability(cap, d).orElse(null);
		return null;
	}

	public static Ingredient createIngredient(ItemStack is) {

		return Ingredient.of(is);
	}

	public static Ingredient createIngredient(ResourceLocation tag) {
		return Ingredient.of(ItemTags.create(tag));
	}

	public static int getEnchantmentLevel(Enchantment enchID, CompoundTag tags) {
		ResourceLocation resourcelocation = CRegistryHelper.getRegistryName(enchID);
		ListTag listnbt = tags.getList("Enchantments", Tag.TAG_COMPOUND);

		for (int i = 0; i < listnbt.size(); ++i) {
			CompoundTag compoundnbt = listnbt.getCompound(i);
			ResourceLocation resourcelocation1 = ResourceLocation.tryParse(compoundnbt.getString("id"));
			if (resourcelocation1 != null && resourcelocation1.equals(resourcelocation)) {
				return Mth.clamp(compoundnbt.getInt("lvl"), 0, 255);
			}
		}

		return 0;
	}

	public static ToIntFunction<BlockState> getLightValueLit(int lightValue) {
		return (state) -> state.getValue(BlockStateProperties.LIT) ? lightValue : 0;
	}

	public static void giveItem(Player pe, ItemStack is) {
		if (!pe.addItem(is))
			pe.level().addFreshEntity(new ItemEntity(pe.level(), pe.blockPosition().getX(), pe.blockPosition().getY(), pe.blockPosition().getZ(), is));
	}

	public static MobEffectInstance noHeal(MobEffectInstance ei) {
		ei.setCurativeItems(ImmutableList.of());
		return ei;
	}

	public static <O, T> Optional<T> ofMap(Map<O, T> map, O key) {
		return Optional.ofNullable(map.get(key));
	}

	public static void spawnMob(ServerLevel world, BlockPos blockpos, CompoundTag nbt, ResourceLocation type) {
		if (Level.isInSpawnableBounds(blockpos)) {
			CompoundTag compoundnbt = nbt.copy();
			compoundnbt.putString("id", type.toString());
			Entity entity = EntityType.loadEntityRecursive(compoundnbt, world, (p_218914_1_) -> {
				p_218914_1_.moveTo(blockpos.getX(), blockpos.getY(), blockpos.getZ(), p_218914_1_.getYRot(), p_218914_1_.getXRot());
				return p_218914_1_;
			});
			if (entity != null) {
				if (entity instanceof Mob) {
					((Mob) entity).finalizeSpawn(world, world.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.NATURAL, null, null);
				}
				if (!world.tryAddFreshEntityWithPassengers(entity)) {
				}
			}
		}
	}

	public static ItemStack Damage(ItemStack stack, int dmg) {
		stack.setDamageValue(dmg);
		return stack;
	}

	public static ItemStack ArmorNBT(ItemStack stack, int base, int mult) {
		stack.setDamageValue((int) (stack.getMaxDamage() - base - Math.random() * mult));
		return stack;
	}

	public static <R extends Recipe<Container>> List<R> filterRecipes(@Nullable RecipeManager recipeManager, RegistryObject<RecipeType<R>> recipeType) {
		if (recipeManager == null)
			recipeManager = CDistHelper.getRecipeManager();
		if (recipeManager == null)
			return ImmutableList.of();
		return recipeManager.getAllRecipesFor(recipeType.get());
	}

	/**
	 * Copying capability uses its serialize and deserialize method to put data to a
	 * new one, as if its saved to world and loaded from world.
	 */
	public static <T extends NBTSerializable> void copyCapability(LazyOptional<T> oldCapability, LazyOptional<T> newCapability) {
		newCapability.ifPresent((newCap) -> oldCapability.ifPresent((oldCap) -> newCap.deserializeNBT(oldCap.serializeNBT())));
	}

	/**
	 * Cloning capability uses reflection to copy all fields within capability,
	 * suitable for capabilities with several transient data The fields are shallow
	 * copy, you should not free resources
	 */
	public static <T extends NBTSerializable> void cloneCapability(LazyOptional<T> oldCapability, LazyOptional<T> newCapability) {
		newCapability.ifPresent((newCap) -> oldCapability.ifPresent((oldCap) -> copyAllFields(newCap, oldCap)));
	}

	/**
	 * Copying capability uses its serialize and deserialize method to put data to a
	 * new one, as if its saved to world and loaded from world.
	 */
	public static <T extends NBTSerializable> void copyPlayerCapability(Capability<T> capability, Player old, Player now) {
		copyCapability(old.getCapability(capability), now.getCapability(capability));
	}

	/**
	 * Cloning capability uses reflection to copy all fields within capability,
	 * suitable for capabilities with several transient data The fields are shallow
	 * copy, you should not free resources
	 */
	public static <T extends NBTSerializable> void clonePlayerCapability(Capability<T> capability, Player old, Player now) {
		cloneCapability(old.getCapability(capability), now.getCapability(capability));
	}

	/**
	 * Copying capability uses its serialize and deserialize method to put data to a
	 * new one, as if its saved to world and loaded from world.
	 */
	public static <T extends NBTSerializable> void copyPlayerCapability(NBTCapabilityType<T> capability, Player old, Player now) {
		copyCapability(capability.getCapability(old), capability.getCapability(now));
	}

	/**
	 * Cloning capability uses reflection to copy all fields within capability,
	 * suitable for capabilities with several transient data The fields are shallow
	 * copy, you should not free resources
	 */
	public static <T extends NBTSerializable> void clonePlayerCapability(NBTCapabilityType<T> capability, Player old, Player now) {
		cloneCapability(capability.getCapability(old), capability.getCapability(now));
	}

	public static <T> void copyAllFields(T to, T from) {
		Class<T> clazz = (Class<T>) from.getClass();
		// OR:
		// Class<T> clazz = (Class<T>) to.getClass();
		List<Field> fields = getAllModelFields(clazz);

		if (fields != null) {
			for (Field field : fields) {
				try {
					field.setAccessible(true);
					field.set(to, field.get(from));
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static List<Field> getAllModelFields(Class<?> aClass) {
		List<Field> fields = new ArrayList<>();
		do {
			Collections.addAll(fields, aClass.getDeclaredFields());
			aClass = aClass.getSuperclass();
		} while (aClass != null);
		fields.removeIf(t -> Modifier.isStatic(t.getModifiers()));
		return fields;
	}

	public static <C extends Collection<T>, T> C copyCollection(@Nonnull C collection) {
		try {
			C copyCollection = (C) collection.getClass().getConstructor().newInstance();
			copyCollection.addAll(collection);
			return copyCollection;

		} catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
			Chorda.LOGGER.error("Failed to copy the collection due to an error: " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}

	public static void setToAirPreserveFluid(LevelAccessor l, BlockPos pos) {
		FluidState curstate = l.getFluidState(pos);
		if (curstate.isEmpty())
			l.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		else
			l.setBlock(pos, curstate.createLegacyBlock(), 2);
	}

	public static boolean isDown(int key) {
		return InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), key);
	}

	public static ThreadFactory makeThreadFactory(String name, boolean isDaemon) {
		AtomicInteger THREAD_NUM = new AtomicInteger(0);
		return r -> {
			Thread th = new Thread(r);
			th.setDaemon(isDaemon);
			th.setName(name + "-" + THREAD_NUM.incrementAndGet());
			return th;
		};
	}

	/**
	 * Pickup fluid from world, respect fluid container settings.
	 * If partially fill is fulfilled, when a container with less than 1000mb space, the fluid block would still be consumed but only produce requested amount of fluid
	 *
	 * @param emptyContainer the container itself
	 * @param playerIn the player
	 * @param level world
	 * @param pos block position
	 * @param side direction
	 * @param allowFillPartial allow consume full fluid block to partially fill container.
	 * @return the fluid action result
	 */
	public static FluidActionResult pickupFluidFromWorld(@NotNull ItemStack emptyContainer, @Nullable Player playerIn, Level level, BlockPos pos, Direction side,boolean allowFillPartial) {

		if (emptyContainer.isEmpty() || level == null || pos == null) {
			return FluidActionResult.FAILURE;
		}

		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		IFluidHandler targetFluidHandler;
		if(allowFillPartial) {
			if (block instanceof IFluidBlock) {
				targetFluidHandler = new FluidBlockWrapper((IFluidBlock) block, level, pos) {
					@Override
					public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
						FluidStack ret= super.drain(new FluidStack(resource,1000), action);
						if(ret.getAmount()==resource.getAmount())
							return ret;
						return new FluidStack(ret,Math.min(ret.getAmount(), resource.getAmount()));
					}

					@Override
					public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
						FluidStack ret= super.drain(1000, action);
						if(ret.getAmount()==maxDrain)
							return ret;
						return new FluidStack(ret,Math.min(ret.getAmount(), maxDrain));
					}
					
				};
			} else if (block instanceof BucketPickup) {
				targetFluidHandler = new BucketPickupHandlerWrapper((BucketPickup) block, level, pos) {
					@Override
					public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
						FluidStack ret= super.drain(new FluidStack(resource,1000), action);
						if(ret.getAmount()==resource.getAmount())
							return ret;
						return new FluidStack(ret,Math.min(ret.getAmount(), resource.getAmount()));
					}

					@Override
					public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
						FluidStack ret= super.drain(1000, action);
						if(ret.getAmount()==maxDrain)
							return ret;
						return new FluidStack(ret,Math.min(ret.getAmount(), maxDrain));
					}
					
				};
			} else return FluidActionResult.FAILURE;
		}else {
			if (block instanceof IFluidBlock) {
				targetFluidHandler = new FluidBlockWrapper((IFluidBlock) block, level, pos);
			} else if (block instanceof BucketPickup) {
				targetFluidHandler = new BucketPickupHandlerWrapper((BucketPickup) block, level, pos);
			} else return FluidActionResult.FAILURE;
		}
		return FluidUtil.tryFillContainer(emptyContainer, targetFluidHandler, Integer.MAX_VALUE, playerIn, true);
	}

}

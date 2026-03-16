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

package com.teammoeg.chorda.util;

import com.google.common.collect.ImmutableList;
import com.teammoeg.chorda.Chorda;
import com.teammoeg.chorda.capability.types.nbt.NBTCapabilityType;
import com.teammoeg.chorda.io.NBTSerializable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Vec3i;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.wrappers.BucketPickupHandlerWrapper;
import net.minecraftforge.fluids.capability.wrappers.FluidBlockWrapper;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;

/**
 * Chorda通用工具类，提供各种Minecraft相关的辅助方法。
 * 包括药水效果应用、方向计算、实体生成、方块实体访问、能力系统操作、
 * 流体拾取、配方过滤等常用功能。
 * <p>
 * General utility class for Chorda, providing various Minecraft-related helper methods.
 * Includes potion effect application, direction calculation, entity spawning, block entity access,
 * capability system operations, fluid pickup, recipe filtering and other common utilities.
 */
public class CUtils {
	/** 资源位置比较器，先按命名空间排序，再按路径排序。 / ResourceLocation comparator, sorts by namespace then by path. */
	public static final Comparator<ResourceLocation> RESOURCE_LOCATION_COMPARATOR = Comparator.comparing(ResourceLocation::getNamespace).thenComparing(ResourceLocation::getPath);

	/**
	 * 将药水效果应用到玩家身上。如果是瞬时效果，则直接应用；否则添加为持续效果。
	 * <p>
	 * Apply a mob effect to a player. If the effect is instantaneous, applies it directly;
	 * otherwise adds it as a lasting effect.
	 *
	 * @param effectinstance 要应用的药水效果实例 / the mob effect instance to apply
	 * @param playerentity 目标玩家 / the target player
	 */
	public static void applyEffectTo(MobEffectInstance effectinstance, Player playerentity) {
		if (effectinstance.getEffect().isInstantenous()) {
			effectinstance.getEffect().applyInstantenousEffect(playerentity, playerentity, playerentity, effectinstance.getAmplifier(), 1.0D);
		} else {
			playerentity.addEffect(new MobEffectInstance(effectinstance));
		}
	}

	/**
	 * 计算从一个方块位置到另一个方块位置的方向。
	 * <p>
	 * Calculate the direction between two block positions.
	 *
	 * @param from 起始位置 / the starting position
	 * @param to 目标位置 / the target position
	 * @return 两个位置之间的方向 / the direction between the two positions
	 */
	public static Direction dirBetween(BlockPos from, BlockPos to) {
		BlockPos delt = from.subtract(to);
		return Direction.fromDelta(Mth.clamp(delt.getX(), -1, 1), Mth.clamp(delt.getY(), -1, 1), Mth.clamp(delt.getZ(), -1, 1));
	}
	/**
	 * 将Vec3向量转换为BlockPos方块位置（截断取整）。
	 * <p>
	 * Convert a Vec3 vector to a BlockPos (truncating to integer).
	 *
	 * @param vec 输入向量 / the input vector
	 * @return 对应的方块位置 / the corresponding block position
	 */
	public static BlockPos vec2Pos(Vec3 vec) {
		return new BlockPos((int)vec.x,(int)vec.y,(int)vec.z);
	}
	/**
	 * 将Vector3f向量转换为BlockPos方块位置（截断取整）。
	 * <p>
	 * Convert a Vector3f vector to a BlockPos (truncating to integer).
	 *
	 * @param vec 输入向量 / the input vector
	 * @return 对应的方块位置 / the corresponding block position
	 */
	public static BlockPos vec2Pos(Vector3f vec) {
		return new BlockPos((int)vec.x,(int)vec.y,(int)vec.z);
	}
	/**
	 * 将Vec3向量转换为对齐的BlockPos方块位置（向下取整）。
	 * <p>
	 * Convert a Vec3 vector to an aligned BlockPos (floor rounding).
	 *
	 * @param vec 输入向量 / the input vector
	 * @return 对齐后的方块位置 / the aligned block position
	 */
	public static BlockPos vec2AlignedPos(Vec3 vec) {
		return new BlockPos((int)Math.floor(vec.x),(int)Math.floor(vec.y),(int)Math.floor(vec.z));
	}
	/**
	 * 将Vector3f向量转换为对齐的BlockPos方块位置（向下取整）。
	 * <p>
	 * Convert a Vector3f vector to an aligned BlockPos (floor rounding).
	 *
	 * @param vec 输入向量 / the input vector
	 * @return 对齐后的方块位置 / the aligned block position
	 */
	public static BlockPos vec2AlignedPos(Vector3f vec) {
		return new BlockPos((int)Math.floor(vec.x),(int)Math.floor(vec.y),(int)Math.floor(vec.z));
	}
	/**
	 * 在指定范围内生成随机方块位置。
	 * <p>
	 * Generate a random block position within the specified bounds.
	 *
	 * @param level 世界实例 / the world instance
	 * @param sizeX X轴范围 / the X-axis bound
	 * @param sizeY Y轴范围 / the Y-axis bound
	 * @param sizeZ Z轴范围 / the Z-axis bound
	 * @return 随机方块位置 / a random block position
	 */
	@Nullable
	public static BlockPos randomPos(Level level,int sizeX,int sizeY,int sizeZ) {
		return new BlockPos(level.random.nextInt(sizeX),level.random.nextInt(sizeY), level.random.nextInt(sizeZ));
	}
	/**
	 * 在指定Vec3i范围内生成随机方块位置。
	 * <p>
	 * Generate a random block position within the specified Vec3i bounds.
	 *
	 * @param level 世界实例 / the world instance
	 * @param size 范围大小 / the size bounds
	 * @return 随机方块位置 / a random block position
	 */
	@Nullable
	public static BlockPos randomPos(Level level,Vec3i size) {
		return randomPos(level,size.getX(),size.getY(),size.getZ());
	}
	/**
	 * 将BlockPos方块位置转换为Vec3向量（取方块下角坐标）。
	 * <p>
	 * Convert a BlockPos to a Vec3 vector (at the lower corner of the block).
	 *
	 * @param pos 方块位置 / the block position
	 * @return 对应的向量 / the corresponding vector
	 */
	public static Vec3 pos2Vec(BlockPos pos) {
		return Vec3.atLowerCornerOf(pos);
	}
	/**
	 * 安全获取已加载区块中的方块实体，避免强制加载区块。
	 * <p>
	 * Safely get a block entity from an already loaded chunk, avoiding forced chunk loading.
	 *
	 * @param world 世界实例 / the world instance
	 * @param pos 方块位置 / the block position
	 * @return 方块实体，如果区块未加载或世界为空则返回null / the block entity, or null if chunk is not loaded or world is null
	 */
	@Nullable
	public static BlockEntity getExistingTileEntity(Level world, BlockPos pos) {
		if (world == null)
			return null;
		if (world.hasChunkAt(pos))
			return world.getBlockEntity(pos);
		return null;
	}

	/**
	 * 安全获取已加载区块中的方块实体，支持LevelAccessor。
	 * <p>
	 * Safely get a block entity from an already loaded chunk, supporting LevelAccessor.
	 *
	 * @param w 世界访问器 / the level accessor
	 * @param pos 方块位置 / the block position
	 * @return 方块实体，如果区块未加载或世界为空则返回null / the block entity, or null if chunk is not loaded or world is null
	 */
	@Nullable
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

	/**
	 * 安全获取指定类型的方块实体。
	 * <p>
	 * Safely get a block entity of the specified type.
	 *
	 * @param <T> 方块实体类型 / the block entity type
	 * @param w 世界访问器 / the level accessor
	 * @param pos 方块位置 / the block position
	 * @param type 目标类型 / the target class type
	 * @return 方块实体，如果类型不匹配或不存在则返回null / the block entity, or null if type mismatch or not present
	 */
	@Nullable
	public static <T> T getExistingTileEntity(LevelAccessor w, BlockPos pos, Class<T> type) {
		BlockEntity te = getExistingTileEntity(w, pos);
		if (type.isInstance(te))
			return (T) te;
		return null;
	}

	/**
	 * 从方块实体获取指定方向的能力。
	 * <p>
	 * Get a capability from a block entity at the specified direction.
	 *
	 * @param <T> 能力类型 / the capability type
	 * @param w 世界访问器 / the level accessor
	 * @param pos 方块位置 / the block position
	 * @param d 访问方向 / the access direction
	 * @param cap 能力实例 / the capability instance
	 * @return 能力对象，不存在则返回null / the capability object, or null if not present
	 */
	@Nullable
	public static <T> T getCapability(LevelAccessor w, BlockPos pos, Direction d, Capability<T> cap) {
		BlockEntity te = getExistingTileEntity(w, pos);
		if (te != null)
			return te.getCapability(cap, d).orElse(null);
		return null;
	}

	/**
	 * 从物品堆创建合成配料。
	 * <p>
	 * Create an Ingredient from an ItemStack.
	 *
	 * @param is 物品堆 / the item stack
	 * @return 合成配料 / the ingredient
	 */
	public static Ingredient createIngredient(ItemStack is) {

		return Ingredient.of(is);
	}

	/**
	 * 从标签创建合成配料。
	 * <p>
	 * Create an Ingredient from a tag ResourceLocation.
	 *
	 * @param tag 标签资源位置 / the tag resource location
	 * @return 合成配料 / the ingredient
	 */
	public static Ingredient createIngredient(ResourceLocation tag) {
		return Ingredient.of(ItemTags.create(tag));
	}

	/**
	 * 从NBT标签中获取指定附魔的等级。
	 * <p>
	 * Get the level of a specific enchantment from NBT tags.
	 *
	 * @param enchID 附魔类型 / the enchantment type
	 * @param tags 包含附魔信息的NBT标签 / the NBT tags containing enchantment data
	 * @return 附魔等级，未找到返回0 / the enchantment level, or 0 if not found
	 */
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

	/**
	 * 获取基于LIT属性的光照值函数，方块点亮时返回指定光照值，否则返回0。
	 * <p>
	 * Get a light value function based on the LIT block state property.
	 * Returns the specified light value when lit, otherwise 0.
	 *
	 * @param lightValue 点亮时的光照值 / the light value when lit
	 * @return 光照值函数 / the light value function
	 */
	public static ToIntFunction<BlockState> getLightValueLit(int lightValue) {
		return (state) -> state.getValue(BlockStateProperties.LIT) ? lightValue : 0;
	}

	/**
	 * 给予玩家物品，如果背包满了则掉落到地上。
	 * <p>
	 * Give an item to a player. If the inventory is full, drop it on the ground.
	 *
	 * @param pe 玩家 / the player
	 * @param is 要给予的物品 / the item to give
	 */
	public static void giveItem(Player pe, ItemStack is) {
		if (!pe.addItem(is))
			pe.level().addFreshEntity(new ItemEntity(pe.level(), pe.blockPosition().getX(), pe.blockPosition().getY(), pe.blockPosition().getZ(), is));
	}

	/**
	 * 移除药水效果的所有治愈物品，使其无法通过喝牛奶等方式移除。
	 * <p>
	 * Remove all curative items from a mob effect, making it impossible to cure with milk etc.
	 *
	 * @param ei 药水效果实例 / the mob effect instance
	 * @return 修改后的药水效果实例 / the modified mob effect instance
	 */
	public static MobEffectInstance noHeal(MobEffectInstance ei) {
		ei.setCurativeItems(ImmutableList.of());
		return ei;
	}

	/**
	 * 从Map中获取值并包装为Optional。
	 * <p>
	 * Get a value from a Map and wrap it in an Optional.
	 *
	 * @param <O> 键类型 / the key type
	 * @param <T> 值类型 / the value type
	 * @param map 源Map / the source map
	 * @param key 要查找的键 / the key to look up
	 * @return 包含值的Optional / an Optional containing the value
	 */
	public static <O, T> Optional<T> ofMap(Map<O, T> map, O key) {
		return Optional.ofNullable(map.get(key));
	}

	/**
	 * 在服务端世界中生成一个生物实体。
	 * <p>
	 * Spawn a mob entity in the server world.
	 *
	 * @param world 服务端世界 / the server level
	 * @param blockpos 生成位置 / the spawn position
	 * @param nbt 实体NBT数据 / the entity NBT data
	 * @param type 实体类型资源位置 / the entity type resource location
	 */
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

	/**
	 * 设置物品堆的损坏值。
	 * <p>
	 * Set the damage value of an ItemStack.
	 *
	 * @param stack 物品堆 / the item stack
	 * @param dmg 损坏值 / the damage value
	 * @return 修改后的物品堆 / the modified item stack
	 */
	public static ItemStack Damage(ItemStack stack, int dmg) {
		stack.setDamageValue(dmg);
		return stack;
	}

	/**
	 * 为盔甲物品设置随机损坏值，模拟已使用状态。
	 * <p>
	 * Set a random damage value for armor items, simulating a used state.
	 *
	 * @param stack 盔甲物品堆 / the armor item stack
	 * @param base 基础耐久偏移量 / the base durability offset
	 * @param mult 随机乘数范围 / the random multiplier range
	 * @return 修改后的物品堆 / the modified item stack
	 */
	public static ItemStack ArmorNBT(ItemStack stack, int base, int mult) {
		stack.setDamageValue((int) (stack.getMaxDamage() - base - Math.random() * mult));
		return stack;
	}

	/**
	 * 获取指定类型的所有配方列表。
	 * <p>
	 * Get all recipes of the specified recipe type.
	 *
	 * @param <C> 容器类型 / the container type
	 * @param <R> 配方类型 / the recipe type
	 * @param recipeManager 配方管理器，为null时自动获取 / the recipe manager, auto-fetched if null
	 * @param recipeType 配方类型注册对象 / the recipe type registry object
	 * @return 匹配的配方列表 / the list of matching recipes
	 */
	public static <C extends Container,R extends Recipe<C>> List<R> filterRecipes(@Nullable RecipeManager recipeManager, RegistryObject<RecipeType<R>> recipeType) {
		if (recipeManager == null)
			recipeManager = CDistHelper.getRecipeManager();
		if (recipeManager == null)
			return ImmutableList.of();
		return recipeManager.getAllRecipesFor(recipeType.get());
	}

	/**
	 * 通过序列化和反序列化的方式复制能力数据，如同存档后重新加载。
	 * <p>
	 * Copy capability data using serialize and deserialize methods,
	 * as if the data was saved to world and loaded from world.
	 *
	 * @param <T> 能力类型 / the capability type
	 * @param oldCapability 源能力 / the source capability
	 * @param newCapability 目标能力 / the target capability
	 */
	public static <T extends NBTSerializable> void copyCapability(LazyOptional<T> oldCapability, LazyOptional<T> newCapability) {
		newCapability.ifPresent((newCap) -> oldCapability.ifPresent((oldCap) -> newCap.deserializeNBT(oldCap.serializeNBT())));
	}

	/**
	 * 使用反射浅拷贝能力的所有字段，适用于包含临时数据的能力。
	 * 字段为浅拷贝，不应释放资源。
	 * <p>
	 * Clone capability by using reflection to shallow-copy all fields,
	 * suitable for capabilities with transient data. Fields are shallow copied,
	 * resources should not be freed.
	 *
	 * @param <T> 能力类型 / the capability type
	 * @param oldCapability 源能力 / the source capability
	 * @param newCapability 目标能力 / the target capability
	 */
	public static <T extends NBTSerializable> void cloneCapability(LazyOptional<T> oldCapability, LazyOptional<T> newCapability) {
		newCapability.ifPresent((newCap) -> oldCapability.ifPresent((oldCap) -> copyAllFields(newCap, oldCap)));
	}

	/**
	 * 通过序列化方式复制玩家能力数据（使用Capability）。
	 * <p>
	 * Copy player capability data via serialization (using Capability).
	 *
	 * @param <T> 能力类型 / the capability type
	 * @param capability 能力实例 / the capability instance
	 * @param old 源玩家 / the source player
	 * @param now 目标玩家 / the target player
	 */
	public static <T extends NBTSerializable> void copyPlayerCapability(Capability<T> capability, Player old, Player now) {
		copyCapability(old.getCapability(capability), now.getCapability(capability));
	}

	/**
	 * 使用反射浅拷贝玩家能力数据（使用Capability）。
	 * <p>
	 * Clone player capability data via reflection shallow-copy (using Capability).
	 *
	 * @param <T> 能力类型 / the capability type
	 * @param capability 能力实例 / the capability instance
	 * @param old 源玩家 / the source player
	 * @param now 目标玩家 / the target player
	 */
	public static <T extends NBTSerializable> void clonePlayerCapability(Capability<T> capability, Player old, Player now) {
		cloneCapability(old.getCapability(capability), now.getCapability(capability));
	}

	/**
	 * 通过序列化方式复制玩家能力数据（使用NBTCapabilityType）。
	 * <p>
	 * Copy player capability data via serialization (using NBTCapabilityType).
	 *
	 * @param <T> 能力类型 / the capability type
	 * @param capability 能力类型实例 / the capability type instance
	 * @param old 源玩家 / the source player
	 * @param now 目标玩家 / the target player
	 */
	public static <T extends NBTSerializable> void copyPlayerCapability(NBTCapabilityType<T> capability, Player old, Player now) {
		copyCapability(capability.getCapability(old), capability.getCapability(now));
	}

	/**
	 * 使用反射浅拷贝玩家能力数据（使用NBTCapabilityType）。
	 * <p>
	 * Clone player capability data via reflection shallow-copy (using NBTCapabilityType).
	 *
	 * @param <T> 能力类型 / the capability type
	 * @param capability 能力类型实例 / the capability type instance
	 * @param old 源玩家 / the source player
	 * @param now 目标玩家 / the target player
	 */
	public static <T extends NBTSerializable> void clonePlayerCapability(NBTCapabilityType<T> capability, Player old, Player now) {
		cloneCapability(capability.getCapability(old), capability.getCapability(now));
	}

	/**
	 * 使用反射将所有非静态字段从源对象浅拷贝到目标对象。
	 * <p>
	 * Shallow-copy all non-static fields from source object to target object using reflection.
	 *
	 * @param <T> 对象类型 / the object type
	 * @param to 目标对象 / the target object
	 * @param from 源对象 / the source object
	 */
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

	/**
	 * 获取指定类及其所有父类的非静态字段列表。
	 * <p>
	 * Get all non-static fields from the specified class and all its superclasses.
	 *
	 * @param aClass 目标类 / the target class
	 * @return 非静态字段列表 / the list of non-static fields
	 */
	public static List<Field> getAllModelFields(Class<?> aClass) {
		List<Field> fields = new ArrayList<>();
		do {
			Collections.addAll(fields, aClass.getDeclaredFields());
			aClass = aClass.getSuperclass();
		} while (aClass != null);
		fields.removeIf(t -> Modifier.isStatic(t.getModifiers()));
		return fields;
	}

	/**
	 * 通过反射创建集合的浅拷贝副本。
	 * <p>
	 * Create a shallow copy of a collection using reflection.
	 *
	 * @param <C> 集合类型 / the collection type
	 * @param <T> 元素类型 / the element type
	 * @param collection 要复制的集合 / the collection to copy
	 * @return 集合的副本，失败时返回null / a copy of the collection, or null on failure
	 */
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

	/**
	 * 将方块设置为空气，但保留原有的流体状态。
	 * <p>
	 * Set a block to air while preserving the existing fluid state.
	 *
	 * @param l 世界访问器 / the level accessor
	 * @param pos 方块位置 / the block position
	 */
	public static void setToAirPreserveFluid(LevelAccessor l, BlockPos pos) {
		FluidState curstate = l.getFluidState(pos);
		if (curstate.isEmpty())
			l.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		else
			l.setBlock(pos, curstate.createLegacyBlock(), 2);
	}

	/**
	 * 创建带有自动命名和守护进程选项的线程工厂。
	 * <p>
	 * Create a thread factory with automatic naming and daemon option.
	 *
	 * @param name 线程名称前缀 / the thread name prefix
	 * @param isDaemon 是否为守护线程 / whether the thread is a daemon thread
	 * @return 线程工厂 / the thread factory
	 */
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
	 * 从世界中拾取流体，尊重流体容器设置。
	 * 当允许部分填充时，即使容器空间不足1000mb，仍会消耗整个流体方块但只填充请求量。
	 * <p>
	 * Pickup fluid from world, respecting fluid container settings.
	 * If partial fill is allowed, when a container has less than 1000mb space,
	 * the fluid block is still consumed but only the requested amount of fluid is produced.
	 *
	 * @param emptyContainer 空容器 / the empty container
	 * @param playerIn 玩家 / the player
	 * @param level 世界 / the world
	 * @param pos 方块位置 / the block position
	 * @param side 方向 / the direction
	 * @param allowFillPartial 是否允许部分填充 / whether to allow partial filling
	 * @return 流体操作结果 / the fluid action result
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
		FluidActionResult result=FluidUtil.tryFillContainer(emptyContainer, targetFluidHandler, Integer.MAX_VALUE, playerIn, true);
		if(result.success) {
			if(emptyContainer.getCount()>1) {
				CUtils.giveItem(playerIn, result.getResult());
				return new FluidActionResult(emptyContainer.copyWithCount(emptyContainer.getCount()-1));
			}
		}
		return result;
	}
	/**
	 * 快速获取指定位置的生物群系（使用噪声生物群系坐标）。
	 * <p>
	 * Quickly get the biome at the specified position (using noise biome coordinates).
	 *
	 * @param level 世界实例 / the world instance
	 * @param pos 方块位置 / the block position
	 * @return 生物群系Holder / the biome holder
	 */
	public static Holder<Biome> fastGetBiome(Level level,BlockPos pos){
		return level.getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()));
	}
	/**
	 * 从已加载区块快速获取指定位置的生物群系。
	 * <p>
	 * Quickly get the biome at the specified position from a loaded chunk.
	 *
	 * @param pChunk 已加载的区块 / the loaded chunk
	 * @param pos 方块位置 / the block position
	 * @return 生物群系Holder / the biome holder
	 */
	public static Holder<Biome> fastGetBiome(LevelChunk pChunk,BlockPos pos){
		return pChunk.getSection(pChunk.getSectionIndex(pos.getY())).getNoiseBiome(QuartPos.fromBlock(pos.getX())&3, QuartPos.fromBlock(pos.getY())&3, QuartPos.fromBlock(pos.getZ())&3);
	}

	/**
	 * 在玩家背包中查找包含指定物品引用的槽位。
	 * <p>
	 * Find the slot in the player's inventory that contains the specified item reference.
	 *
	 * @param player 玩家 / the player
	 * @param item 要查找的物品引用 / the item reference to find
	 * @return 包含该物品的槽位，未找到返回null / the slot containing the item, or null if not found
	 */
	public static Slot getItemSlotInPlayerInv(Player player, ItemStack item) {
		for (Slot slot : player.inventoryMenu.slots) {
			if (slot.getItem() == item) {
				return slot;
			}
		}
		return null;
	}

	/**
	 * 根据UUID从服务端世界获取实体。
	 * <p>
	 * Get an entity from the server level by UUID.
	 *
	 * @param level 服务端世界 / the server level
	 * @param uuid 实体UUID / the entity UUID
	 * @return 实体，未找到返回null / the entity, or null if not found
	 */
	@Nullable
	public static Entity getEntity(ServerLevel level, UUID uuid) {
		return level.getEntity(uuid);
	}
}

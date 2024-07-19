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

package com.teammoeg.frostedheart.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.ToIntFunction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.base.capability.nbt.FHNBTCapability;
import com.teammoeg.frostedheart.content.climate.WorldClimate;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.heatdevice.chunkheatdata.ChunkHeatData;
import com.teammoeg.frostedheart.util.client.ClientUtils;
import com.teammoeg.frostedheart.util.io.NBTSerializable;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.ItemTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.crafting.NBTIngredient;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;

public class FHUtils {
    private static class NBTIngredientAccess extends NBTIngredient {
        public NBTIngredientAccess(ItemStack stack) {
            super(stack);
        }
    }

    private static final ResourceLocation emptyLoot = new ResourceLocation("frostedheart:empty");
	public static final String NBT_HEATER_VEST = FHMain.MODID + "heater_vest";
	public static final String FIRST_LOGIN_GIVE_NUTRITION = FHMain.MODID + "first_login_give_nutrition";
	public static final String FIRST_LOGIN_GIVE_MANUAL = "first";

    public static void applyEffectTo(EffectInstance effectinstance, PlayerEntity playerentity) {
        if (effectinstance.getPotion().isInstant()) {
            effectinstance.getPotion().affectEntity(playerentity, playerentity, playerentity, effectinstance.getAmplifier(), 1.0D);
        } else {
            playerentity.addPotionEffect(new EffectInstance(effectinstance));
        }
    }

    public static boolean canBigTreeGenerate(World w, BlockPos p, Random r) {

        return canTreeGenerate(w, p, r, 7);

    }

    public static void canBigTreeGenerate(World w, BlockPos p, Random r, CallbackInfoReturnable<Boolean> cr) {
        if (!canBigTreeGenerate(w, p, r))
            cr.setReturnValue(false);
    }

    public static boolean canGrassSurvive(IWorldReader world, BlockPos pos) {
        float t = ChunkHeatData.getTemperature(world, pos);
        return t >= WorldTemperature.HEMP_GROW_TEMPERATURE && t <= WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX;
    }

    public static boolean canNetherTreeGrow(IBlockReader w, BlockPos p) {
        if (!(w instanceof IWorld)) {
            return false;
        }
        float temp = ChunkHeatData.getTemperature((IWorld) w, p);
        if (temp <= 300)
            return false;
        return !(temp > 300 + WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX);
    }

    public static boolean canTreeGenerate(World w, BlockPos p, Random r, int chance) {
        return r.nextInt(chance) == 0;

    }
    public static Direction dirBetween(BlockPos from,BlockPos to) {
    	BlockPos delt=from.subtract(to);
    	return Direction.byLong(MathHelper.clamp(delt.getX(), -1, 1), MathHelper.clamp(delt.getY(), -1, 1), MathHelper.clamp(delt.getZ(), -1, 1));
    }
    public static TileEntity getExistingTileEntity(IWorld w,BlockPos pos) {
		if(w==null)
			return null;
    	TileEntity te=null;
    	if(w instanceof World) {
    		te=Utils.getExistingTileEntity((World) w, pos);
    	}else {
			if(w.isBlockLoaded(pos))
				te=w.getTileEntity(pos);
    	}
    	return te;
    }
    public static <T> T getExistingTileEntity(IWorld w,BlockPos pos,Class<T> type) {
    	TileEntity te=getExistingTileEntity(w,pos);
    	if(type.isInstance(te))
    		return (T) te;
    	return null;
    }

    public static <T> T getCapability(IWorld w,BlockPos pos,Direction d,Capability<T> cap){
    	TileEntity te=getExistingTileEntity(w,pos);
    	if(te!=null)
    		return te.getCapability(cap,d).orElse(null);
    	return null;
    }
    public static boolean canTreeGrow(World w, BlockPos p, Random r) {
        float temp = ChunkHeatData.getTemperature(w, p);
        if (temp <= -6 || WorldClimate.isBlizzard(w))
            return false;
        if (temp > WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX)
            return false;
        if (temp > 0)
            return true;
        return r.nextInt(Math.max(1, MathHelper.ceil(-temp / 2))) == 0;
    }
    public static boolean hasItems(PlayerEntity player,List<IngredientWithSize> costList) {
    	int i=0;
        for (IngredientWithSize iws : costList) {
            int count = iws.getCount();
            for (ItemStack it : player.inventory.mainInventory) {
                if (iws.testIgnoringSize(it)) {
                    count -= it.getCount();
                    if (count <= 0)
                        break;
                }
            }
            if (count > 0) {
            	return false;
            }
        }
        return true;
    }
    public static BitSet checkItemList(PlayerEntity player,List<IngredientWithSize> costList) {
    	BitSet bs=new BitSet(costList.size());
    	int i=0;
        for (IngredientWithSize iws : costList) {
            int count = iws.getCount();
            for (ItemStack it : player.inventory.mainInventory) {
                if (iws.testIgnoringSize(it)) {
                    count -= it.getCount();
                    if (count <= 0)
                        break;
                }
            }
            if (count > 0) {
            	bs.set(i++,false);
            } else {
            	bs.set(i++, true);
            }
        }
        return bs;
    }
    public static boolean costItems(PlayerEntity player,List<IngredientWithSize> costList) {
        // first do simple verify
        for (IngredientWithSize iws : costList) {
            int count = iws.getCount();
            for (ItemStack it : player.inventory.mainInventory) {
                if (iws.testIgnoringSize(it)) {
                    count -= it.getCount();
                    if (count <= 0)
                        break;
                }
            }
            if (count > 0)
                return false;
        }
        System.out.println("test");
        // then really consume item
        List<ItemStack> ret = new ArrayList<>();
        for (IngredientWithSize iws : costList) {
            int count = iws.getCount();
            for (ItemStack it : player.inventory.mainInventory) {
                if (iws.testIgnoringSize(it)) {
                    int redcount = Math.min(count, it.getCount());
                    ret.add(it.split(redcount));
                    count -= redcount;
                    if (count <= 0)
                        break;
                }
            }
            if (count > 0) {// wrong, revert.
                for (ItemStack it : ret)
                    FHUtils.giveItem(player, it);
                return false;
            }
        }
        return true;
    }
    public static Ingredient createIngredient(ItemStack is) {
        if (is.hasTag()) return new NBTIngredientAccess(is);
        return Ingredient.fromStacks(is);
    }

    public static Ingredient createIngredient(ResourceLocation tag) {
        return Ingredient.fromTag(ItemTags.getCollection().get(tag));
    }

    public static IngredientWithSize createIngredientWithSize(ItemStack is) {
        if (is.hasTag()) return new IngredientWithSize(new NBTIngredientAccess(is), is.getCount());
        return new IngredientWithSize(Ingredient.fromStacks(is), is.getCount());
    }

    public static IngredientWithSize createIngredientWithSize(ResourceLocation tag, int count) {
        return new IngredientWithSize(Ingredient.fromTag(ItemTags.getCollection().get(tag)), count);
    }

    public static ResourceLocation getEmptyLoot() {
        return emptyLoot;
    }

    public static int getEnchantmentLevel(Enchantment enchID, CompoundNBT tags) {
        ResourceLocation resourcelocation = RegistryUtils.getRegistryName(enchID);
        ListNBT listnbt = tags.getList("Enchantments", Constants.NBT.TAG_COMPOUND);

        for (int i = 0; i < listnbt.size(); ++i) {
            CompoundNBT compoundnbt = listnbt.getCompound(i);
            ResourceLocation resourcelocation1 = ResourceLocation.tryCreate(compoundnbt.getString("id"));
            if (resourcelocation1 != null && resourcelocation1.equals(resourcelocation)) {
                return MathHelper.clamp(compoundnbt.getInt("lvl"), 0, 255);
            }
        }

        return 0;
    }

    public static ToIntFunction<BlockState> getLightValueLit(int lightValue) {
        return (state) -> state.get(BlockStateProperties.LIT) ? lightValue : 0;
    }

    public static void giveItem(PlayerEntity pe, ItemStack is) {
        if (!pe.addItemStackToInventory(is))
            pe.world.addEntity(new ItemEntity(pe.world, pe.getPosition().getX(), pe.getPosition().getY(), pe.getPosition().getZ(), is));
    }

    public static boolean isBlizzardHarming(IWorld iWorld, BlockPos p) {
        return WorldClimate.isBlizzard(iWorld) && iWorld.getHeight(Type.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ()) <= p.getY();
    }

    public static boolean isRainingAt(BlockPos pos, World world) {

        if (!world.isRaining()) {
            return false;
        } else if (!world.canSeeSky(pos)) {
            return false;
        } else return world.getHeight(Type.MOTION_BLOCKING, pos).getY() <= pos.getY();
    }

    public static EffectInstance noHeal(EffectInstance ei) {
        ei.setCurativeItems(ImmutableList.of());
        return ei;
    }

    public static <T> T notNull() {
        return null;
    }

    public static <O, T> Optional<T> ofMap(Map<O, T> map, O key) {
        return Optional.ofNullable(map.get(key));
    }

    public static void spawnMob(ServerWorld world, BlockPos blockpos, CompoundNBT nbt, ResourceLocation type) {
        if (World.isInvalidPosition(blockpos)) {
            CompoundNBT compoundnbt = nbt.copy();
            compoundnbt.putString("id", type.toString());
            Entity entity = EntityType.loadEntityAndExecute(compoundnbt, world, (p_218914_1_) -> {
                p_218914_1_.setLocationAndAngles(blockpos.getX(), blockpos.getY(), blockpos.getZ(), p_218914_1_.rotationYaw, p_218914_1_.rotationPitch);
                return p_218914_1_;
            });
            if (entity != null) {
                if (entity instanceof MobEntity) {
                    ((MobEntity) entity).onInitialSpawn(world, world.getDifficultyForLocation(entity.getPosition()), SpawnReason.NATURAL, null, null);
                }
                if (!world.addEntityAndUniquePassengers(entity)) {
                }
            }
        }
    }

	public static ItemStack Damage(ItemStack stack, int dmg) {
	    stack.setDamage(dmg);
	    return stack;
	}

	public static ItemStack ArmorNBT(ItemStack stack, int base, int mult) {
	    stack.setDamage((int) (stack.getMaxDamage() - base - Math.random() * mult));
	    return stack;
	}
	public static <R extends IRecipe<IInventory>> List<R> filterRecipes(@Nullable RecipeManager recipeManager, IRecipeType<R> recipeType) {
        if(recipeManager==null) {
        	if(ClientUtils.mc().world!=null)
        		recipeManager=ClientUtils.mc().world.getRecipeManager();
        }
        if(recipeManager==null)
        	return ImmutableList.of();
		return recipeManager.getRecipesForType(recipeType);
    }
	public static ItemStack ArmorLiningNBT(ItemStack stack) {
	    stack.getOrCreateTag().putString("inner_cover", "frostedheart:straw_lining");
	    stack.getTag().putBoolean("inner_bounded", true);//bound lining to arm or
	    return ArmorNBT(stack, 107, 6);
	}
   public static <T extends NBTSerializable> void copyCapability(LazyOptional<T> oldCapability, LazyOptional<T> newCapability){
       newCapability.ifPresent((newCap) -> oldCapability.ifPresent((oldCap) -> newCap.deserializeNBT(oldCap.serializeNBT())));
   }
   public static <T extends NBTSerializable> void cloneCapability(LazyOptional<T> oldCapability, LazyOptional<T> newCapability){
       newCapability.ifPresent((newCap) -> oldCapability.ifPresent((oldCap) -> copyAllFields(newCap,oldCap)));
   }
   public static <T extends NBTSerializable> void copyPlayerCapability(Capability<T> capability,PlayerEntity old,PlayerEntity now){
	   copyCapability(old.getCapability(capability),now.getCapability(capability));
   }
   public static <T extends NBTSerializable> void clonePlayerCapability(Capability<T> capability,PlayerEntity old,PlayerEntity now){
	   cloneCapability(old.getCapability(capability),now.getCapability(capability));
   }
   public static <T extends NBTSerializable> void copyPlayerCapability(FHNBTCapability<T> capability,PlayerEntity old,PlayerEntity now){
	   copyCapability(capability.getCapability(old),capability.getCapability(now));
   }
   public static <T extends NBTSerializable> void clonePlayerCapability(FHNBTCapability<T> capability,PlayerEntity old,PlayerEntity now){
	   cloneCapability(capability.getCapability(old),capability.getCapability(now));
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
                   field.set(to,field.get(from));
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
	    return fields;
	}

    public static <C extends Collection<T>, T> C copyCollection(@Nonnull C collection){
        try {
            C copyCollection = (C) collection.getClass().getConstructor().newInstance();
            copyCollection.addAll(collection);
            return copyCollection;

        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            System.err.println("Failed to copy the collection due to an error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.LazyOptional;

public class FHUtils {

    private static final ResourceLocation emptyLoot = new ResourceLocation("frostedheart:empty");
	public static final String NBT_HEATER_VEST = FHMain.MODID + "heater_vest";
	public static final String FIRST_LOGIN_GIVE_NUTRITION = FHMain.MODID + "first_login_give_nutrition";
	public static final String FIRST_LOGIN_GIVE_MANUAL = "first";

    public static void applyEffectTo(MobEffectInstance effectinstance, Player playerentity) {
        if (effectinstance.getEffect().isInstantenous()) {
            effectinstance.getEffect().applyInstantenousEffect(playerentity, playerentity, playerentity, effectinstance.getAmplifier(), 1.0D);
        } else {
            playerentity.addEffect(new MobEffectInstance(effectinstance));
        }
    }

    public static boolean canBigTreeGenerate(Level w, BlockPos p, Random r) {

        return canTreeGenerate(w, p, r, 7);

    }

    public static void canBigTreeGenerate(Level w, BlockPos p, Random r, CallbackInfoReturnable<Boolean> cr) {
        if (!canBigTreeGenerate(w, p, r))
            cr.setReturnValue(false);
    }

    public static boolean canGrassSurvive(LevelReader world, BlockPos pos) {
        float t = ChunkHeatData.getTemperature(world, pos);
        return t >= WorldTemperature.HEMP_GROW_TEMPERATURE && t <= WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX;
    }

    public static boolean canNetherTreeGrow(BlockGetter w, BlockPos p) {
        if (!(w instanceof LevelAccessor)) {
            return false;
        }
        float temp = ChunkHeatData.getTemperature((LevelAccessor) w, p);
        if (temp <= 300)
            return false;
        return !(temp > 300 + WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX);
    }

    public static boolean canTreeGenerate(Level w, BlockPos p, Random r, int chance) {
        return r.nextInt(chance) == 0;

    }
    public static Direction dirBetween(BlockPos from,BlockPos to) {
    	BlockPos delt=from.subtract(to);
    	return Direction.fromDelta(Mth.clamp(delt.getX(), -1, 1), Mth.clamp(delt.getY(), -1, 1), Mth.clamp(delt.getZ(), -1, 1));
    }
    public static BlockEntity getExistingTileEntity(LevelAccessor w,BlockPos pos) {
		if(w==null)
			return null;
    	BlockEntity te=null;
    	if(w instanceof Level) {
    		te=Utils.getExistingTileEntity((Level) w, pos);
    	}else {
			if(w.hasChunkAt(pos))
				te=w.getBlockEntity(pos);
    	}
    	return te;
    }
    public static <T> T getExistingTileEntity(LevelAccessor w,BlockPos pos,Class<T> type) {
    	BlockEntity te=getExistingTileEntity(w,pos);
    	if(type.isInstance(te))
    		return (T) te;
    	return null;
    }

    public static <T> T getCapability(LevelAccessor w,BlockPos pos,Direction d,Capability<T> cap){
    	BlockEntity te=getExistingTileEntity(w,pos);
    	if(te!=null)
    		return te.getCapability(cap,d).orElse(null);
    	return null;
    }
    public static boolean canTreeGrow(Level w, BlockPos p, Random r) {
        float temp = ChunkHeatData.getTemperature(w, p);
        if (temp <= -6 || WorldClimate.isBlizzard(w))
            return false;
        if (temp > WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX)
            return false;
        if (temp > 0)
            return true;
        return r.nextInt(Math.max(1, Mth.ceil(-temp / 2))) == 0;
    }
    public static boolean hasItems(Player player,List<IngredientWithSize> costList) {
    	int i=0;
        for (IngredientWithSize iws : costList) {
            int count = iws.getCount();
            for (ItemStack it : player.getInventory().items) {
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
    public static BitSet checkItemList(Player player,List<IngredientWithSize> costList) {
    	BitSet bs=new BitSet(costList.size());
    	int i=0;
        for (IngredientWithSize iws : costList) {
            int count = iws.getCount();
            for (ItemStack it : player.getInventory().items) {
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
    public static boolean costItems(Player player,List<IngredientWithSize> costList) {
        // first do simple verify
        for (IngredientWithSize iws : costList) {
            int count = iws.getCount();
            for (ItemStack it : player.getInventory().items) {
                if (iws.testIgnoringSize(it)) {
                    count -= it.getCount();
                    if (count <= 0)
                        break;
                }
            }
            if (count > 0)
                return false;
        }
        //System.out.println("test");
        // then really consume item
        List<ItemStack> ret = new ArrayList<>();
        for (IngredientWithSize iws : costList) {
            int count = iws.getCount();
            for (ItemStack it : player.getInventory().items) {
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

        return Ingredient.of(is);
    }

    public static Ingredient createIngredient(ResourceLocation tag) {
        return Ingredient.of(ItemTags.create(tag));
    }

    public static IngredientWithSize createIngredientWithSize(ItemStack is) {
        return new IngredientWithSize(createIngredient(is), is.getCount());
    }

    public static IngredientWithSize createIngredientWithSize(ResourceLocation tag, int count) {
        return new IngredientWithSize(createIngredient(tag), count);
    }

    public static ResourceLocation getEmptyLoot() {
        return emptyLoot;
    }

    public static int getEnchantmentLevel(Enchantment enchID, CompoundTag tags) {
        ResourceLocation resourcelocation = RegistryUtils.getRegistryName(enchID);
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

    public static boolean isBlizzardHarming(LevelAccessor iWorld, BlockPos p) {
        return WorldClimate.isBlizzard(iWorld) && iWorld.getHeight(Types.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ()) <= p.getY();
    }

    public static boolean isRainingAt(BlockPos pos, Level world) {

        if (!world.isRaining()) {
            return false;
        } else if (!world.canSeeSky(pos)) {
            return false;
        } else return world.getHeightmapPos(Types.MOTION_BLOCKING, pos).getY() <= pos.getY();
    }

    public static MobEffectInstance noHeal(MobEffectInstance ei) {
        ei.setCurativeItems(ImmutableList.of());
        return ei;
    }

    public static <T> T notNull() {
        return null;
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
	public static <R extends Recipe<Container>> List<R> filterRecipes(@Nullable RecipeManager recipeManager, RecipeType<R> recipeType) {
        if(recipeManager==null) {
        	if(ClientUtils.mc().level!=null)
        		recipeManager=ClientUtils.mc().level.getRecipeManager();
        }
        if(recipeManager==null)
        	return ImmutableList.of();
		return recipeManager.getAllRecipesFor(recipeType);
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
   public static <T extends NBTSerializable> void copyPlayerCapability(Capability<T> capability,Player old,Player now){
	   copyCapability(old.getCapability(capability),now.getCapability(capability));
   }
   public static <T extends NBTSerializable> void clonePlayerCapability(Capability<T> capability,Player old,Player now){
	   cloneCapability(old.getCapability(capability),now.getCapability(capability));
   }
   public static <T extends NBTSerializable> void copyPlayerCapability(FHNBTCapability<T> capability,Player old,Player now){
	   copyCapability(capability.getCapability(old),capability.getCapability(now));
   }
   public static <T extends NBTSerializable> void clonePlayerCapability(FHNBTCapability<T> capability,Player old,Player now){
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

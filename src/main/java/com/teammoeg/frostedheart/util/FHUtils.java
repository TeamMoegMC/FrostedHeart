/*
 * Copyright (c) 2021 TeamMoeg
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
 */

package com.teammoeg.frostedheart.util;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.ToIntFunction;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.common.collect.ImmutableList;
import com.teammoeg.frostedheart.climate.WorldClimate;
import com.teammoeg.frostedheart.climate.WorldTemperature;
import com.teammoeg.frostedheart.climate.chunkheatdata.ChunkHeatData;

import blusunrize.immersiveengineering.api.crafting.IngredientWithSize;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.crafting.NBTIngredient;

public class FHUtils {
    private static class NBTIngredientAccess extends NBTIngredient {
        public NBTIngredientAccess(ItemStack stack) {
            super(stack);
        }
    }

    public static <T> T notNull() {
        return null;
    }
    public static void applyEffectTo(EffectInstance effectinstance,PlayerEntity playerentity) {
    	if (effectinstance.getPotion().isInstant()) {
            effectinstance.getPotion().affectEntity(playerentity, playerentity, playerentity, effectinstance.getAmplifier(), 1.0D);
         } else {
        	 playerentity.addPotionEffect(new EffectInstance(effectinstance));
         }
    }
    public static Ingredient createIngredient(ItemStack is) {
        if (is.hasTag()) return new NBTIngredientAccess(is);
        return Ingredient.fromStacks(is);
    }

    public static IngredientWithSize createIngredientWithSize(ResourceLocation tag, int count) {
        return new IngredientWithSize(Ingredient.fromTag(ItemTags.getCollection().get(tag)), count);
    }

    public static IngredientWithSize createIngredientWithSize(ItemStack is) {
        if (is.hasTag()) return new IngredientWithSize(new NBTIngredientAccess(is), is.getCount());
        return new IngredientWithSize(Ingredient.fromStacks(is), is.getCount());
    }

    public static Ingredient createIngredient(ResourceLocation tag) {
        return Ingredient.fromTag(ItemTags.getCollection().get(tag));
    }

    public static void giveItem(PlayerEntity pe, ItemStack is) {
        if (!pe.addItemStackToInventory(is))
            pe.world.addEntity(new ItemEntity(pe.world, pe.getPosition().getX(), pe.getPosition().getY(), pe.getPosition().getZ(), is));
    }

    public static void registerSimpleCapability(Class<?> clazz) {
        CapabilityManager.INSTANCE.register(clazz, new NoopStorage<>(), () -> {
            throw new UnsupportedOperationException("Creating default instances is not supported. Why would you ever do this");
        });
    }

    public static ToIntFunction<BlockState> getLightValueLit(int lightValue) {
        return (state) -> {
            return state.get(BlockStateProperties.LIT) ? lightValue : 0;
        };
    }

    public static boolean isRainingAt(BlockPos pos, World world) {
        if (!world.isRaining()) {
            return false;
        } else if (!world.canSeeSky(pos)) {
            return false;
        } else if (world.getHeight(Heightmap.Type.MOTION_BLOCKING, pos).getY() > pos.getY()) {
            return false;
        } else {
            return true;
        }
    }

    public static void canBigTreeGenerate(World w, BlockPos p, Random r, CallbackInfoReturnable<Boolean> cr) {
        if (!canBigTreeGenerate(w, p, r))
            cr.setReturnValue(false);
    }

    public static boolean canTreeGenerate(World w, BlockPos p, Random r,int chance) {
        return r.nextInt(chance) == 0;

    }
    public static boolean isBlizzardHarming(IWorld iWorld, BlockPos p) {
    	if(WorldClimate.isBlizzard(iWorld)&&iWorld.getHeight(Type.MOTION_BLOCKING_NO_LEAVES,p.getX(),p.getZ())<=p.getY()) {
    		return true;
    	}
    	return false;
    }
    public static boolean canTreeGrow(World w, BlockPos p, Random r) {
        float temp=ChunkHeatData.getTemperature(w, p);
        if(temp<=-6||WorldClimate.isBlizzard(w))
        	return false;
        if(temp>WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX)
        	return false;
        if(temp>0)
        	return true;
    	return r.nextInt(Math.max(1,MathHelper.ceil(-temp/2))) == 0;
    }
    public static boolean canNetherTreeGrow(IBlockReader w, BlockPos p) {
    	if(!(w instanceof IWorld)) {
    		return false;
    	}
        float temp=ChunkHeatData.getTemperature((IWorld) w, p);
        if(temp<=300)
        	return false;
        if(temp>300+WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX)
        	return false;
        return true;
    }
    public static boolean canBigTreeGenerate(World w, BlockPos p, Random r) {

        return canTreeGenerate(w,p,r,7);

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
                    ((MobEntity) entity).onInitialSpawn(world, world.getDifficultyForLocation(entity.getPosition()), SpawnReason.NATURAL, (ILivingEntityData) null, (CompoundNBT) null);
                }
                if (!world.addEntityAndUniquePassengers(entity)) {
                    return;
                }
            }
        }
    }

    public static int getEnchantmentLevel(Enchantment enchID, CompoundNBT tags) {
        ResourceLocation resourcelocation = Registry.ENCHANTMENT.getKey(enchID);
        ListNBT listnbt = tags.getList("Enchantments", 10);

        for (int i = 0; i < listnbt.size(); ++i) {
            CompoundNBT compoundnbt = listnbt.getCompound(i);
            ResourceLocation resourcelocation1 = ResourceLocation.tryCreate(compoundnbt.getString("id"));
            if (resourcelocation1 != null && resourcelocation1.equals(resourcelocation)) {
                return MathHelper.clamp(compoundnbt.getInt("lvl"), 0, 255);
            }
        }

        return 0;
    }

    public static EffectInstance noHeal(EffectInstance ei) {
        ei.setCurativeItems(ImmutableList.of());
        return ei;
    }

    public static boolean canGrassSurvive(IWorldReader world, BlockPos pos) {
        float t = ChunkHeatData.getTemperature(world, pos);
        return t >= WorldTemperature.HEMP_GROW_TEMPERATURE && t <= WorldTemperature.VANILLA_PLANT_GROW_TEMPERATURE_MAX;
    }
    public static <O,T> Optional<T> ofMap(Map<O,T> map,O key){
    	return Optional.ofNullable(map.get(key));
    }
}

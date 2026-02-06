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

package com.teammoeg.frostedheart.content.water.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.teammoeg.frostedheart.FHMain;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class WaterLevelAndEffectRecipe implements Recipe<Inventory>, Comparable<WaterLevelAndEffectRecipe> {
    protected final int waterLevel, waterSaturationLevel;
    protected final ResourceLocation id;
    protected final List<MobEffectInstance> mobEffectInstances;
    protected final String group;
    protected final Ingredient ingredient;
    protected final Fluid fluid;
    protected final CompoundTag compoundTag;
    protected float priority = 0;
    protected final int duration, amplifier;
    protected float probability;


    public static RegistryObject<RecipeSerializer<WaterLevelAndEffectRecipe>> SERIALIZER;
    public static RegistryObject<RecipeType<WaterLevelAndEffectRecipe>> TYPE;

    public static class Serializer implements RecipeSerializer<WaterLevelAndEffectRecipe> {

        public WaterLevelAndEffectRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            /* Default if values not be written
             * nbt null
             * fluid null
             * mobEffects List(Empty)
             * ingredient Ingredient.EMPTY
             * waterLevel 0
             * waterSaturationLevel 0
             * duration  0
             * amplifier 0
             * probability 0
             * */
            int waterLevel = 0;
            int waterSaturationLevel = 0;
            Fluid fluid = null;
            CompoundTag compoundTag = null;
            List<MobEffectInstance> effectInstances = new ArrayList<>();

            String group = GsonHelper.getAsString(json, "group", "");

            //ingredient
            Ingredient ingredient = Ingredient.EMPTY;
            if (GsonHelper.isValidNode(json,"ingredient")) {
                JsonElement jsonelement = GsonHelper.isArrayNode(json, "ingredient") ? GsonHelper.getAsJsonArray(json, "ingredient") : GsonHelper.getAsJsonObject(json, "ingredient");
                ingredient = Ingredient.fromJson(jsonelement);
            }


            //mobEffects
            if (GsonHelper.isArrayNode(json ,"mob_effects")){
                JsonArray effectsJsonArray = GsonHelper.getAsJsonArray(json, "mob_effects");
                for (JsonElement effect : effectsJsonArray) {
                    JsonObject mobEffectJsonObj = effect.getAsJsonObject();
                    int duration = GsonHelper.getAsInt(mobEffectJsonObj, "duration");
                    int amplifier = GsonHelper.getAsInt(mobEffectJsonObj, "amplifier");
                    String name = GsonHelper.getAsString(mobEffectJsonObj, "name");
                    if (duration > 0 && amplifier >= 0) {
                        MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(name));
                        if (mobEffect != null) {
                            effectInstances.add(new MobEffectInstance(mobEffect, duration, amplifier));
                        }
                    }
                }
            }


            //nbt
            if (GsonHelper.isValidNode(json ,"nbt")) {
                JsonObject nbt = GsonHelper.getAsJsonObject(json, "nbt");
                try {
                    compoundTag = NbtUtils.snbtToStructure(nbt.toString());
                } catch (CommandSyntaxException e) {
                    FHMain.LOGGER.error("Water level recipe " + recipeId + " has no nbt.");
                }
                //todo debug
            }

            //fluid
            if (GsonHelper.isValidNode(json ,"fluid")){
                String fluidName = GsonHelper.getAsString(json, "fluid", "");
                fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluidName));
            }
            //thrist
            int duration = GsonHelper.getAsInt(json, "duration", 0);
            int amplifier = GsonHelper.getAsInt(json, "amplifier", 0);
            float probability = GsonHelper.getAsFloat(json, "probability", 0);
            //water level
            waterLevel = GsonHelper.getAsInt(json, "waterLevel", 0);
            waterSaturationLevel = GsonHelper.getAsInt(json, "waterSaturationLevel", 0);

            return new WaterLevelAndEffectRecipe(recipeId, group, ingredient, waterLevel, waterSaturationLevel, effectInstances, fluid, compoundTag,duration ,amplifier,probability );
        }

        @Override
        public WaterLevelAndEffectRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf packetBuffer) {
            List<MobEffectInstance> mobEffectInstances = new ArrayList<>();
            String group = packetBuffer.readUtf();//1
            Ingredient ingredient = Ingredient.fromNetwork(packetBuffer);//2
            int waterLevel = packetBuffer.readVarInt();//3
            int waterSaturationLevel = packetBuffer.readVarInt();//4
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(packetBuffer.readUtf()));//5
            CompoundTag compoundTag = packetBuffer.readNbt();//6
            int duration = packetBuffer.readVarInt();
            int amplifier = packetBuffer.readVarInt();
            float probability = packetBuffer.readFloat();
            int count=packetBuffer.readByte();
            for (int i = 0; i < count; i++) {
                String mobEffectName = packetBuffer.readUtf();
                int duratione = packetBuffer.readVarInt();
                int amplifiere = packetBuffer.readVarInt();

                MobEffect mobEffect = ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.tryParse(mobEffectName));
                mobEffectInstances.add(new MobEffectInstance(mobEffect, duratione, amplifiere));
            }
            return new WaterLevelAndEffectRecipe(recipeId, group, ingredient, waterLevel, waterSaturationLevel, mobEffectInstances, fluid, compoundTag,duration ,amplifier,probability);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, WaterLevelAndEffectRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            recipe.getIngredient().toNetwork(buffer);
            buffer.writeVarInt(recipe.getWaterLevel());
            buffer.writeVarInt(recipe.getWaterSaturationLevel());
            buffer.writeUtf(recipe.getFluid() == null ? "" : ForgeRegistries.FLUIDS.getKey(recipe.getFluid()).toString());
            buffer.writeNbt(recipe.getCompoundTag());
            buffer.writeVarInt(recipe.getDuration());
            buffer.writeVarInt(recipe.getAmplifier());
            buffer.writeFloat(recipe.getProbability());


            buffer.writeByte(recipe.getMobEffectInstances().size());
            for (MobEffectInstance mobEffectInstance : recipe.getMobEffectInstances()) {
                buffer.writeUtf(ForgeRegistries.MOB_EFFECTS.getKey(mobEffectInstance.getEffect()).toString());
                buffer.writeVarInt(mobEffectInstance.getDuration());
                buffer.writeVarInt(mobEffectInstance.getAmplifier());
            }
        }
    }

    public WaterLevelAndEffectRecipe(ResourceLocation idIn, String groupIn, Ingredient ingredient, int waterLevel, int waterSaturationLevel, List<MobEffectInstance> effectInstances, Fluid fluid, CompoundTag compoundTag, int duration, int amplifier,float probability) {
        this.id = idIn;
        this.group = groupIn;
        this.waterLevel = waterLevel;
        this.waterSaturationLevel = waterSaturationLevel;
        this.ingredient = ingredient;
        this.mobEffectInstances = effectInstances;
        this.fluid = fluid;
        this.compoundTag = compoundTag;
        this.duration = duration;
        this.amplifier = amplifier;
        this.probability = probability;

        float priority = 0;
        if (!ingredient.isEmpty()) priority += 1.5F;
        if (compoundTag != null) priority += 1F;
        if (fluid != null) priority += 1F;
        if (effectInstances.size() > 0) priority += 0.1F;

        this.priority = priority;
    }

    public int getAmplifier() {
        return amplifier;
    }

    public int getDuration() {
        return duration;
    }

    public float getProbability() {return probability;}

    public int getWaterSaturationLevel() {
        return waterSaturationLevel;
    }

    public int getWaterLevel() {
        return waterLevel;
    }

    public List<MobEffectInstance> getMobEffectInstances() {
        return mobEffectInstances;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public CompoundTag getCompoundTag() {
        return compoundTag;
    }

    public float getPriority() {
        return priority;
    }

    public boolean conform(ItemStack conformStack) {
        IFluidHandler fluidHandler = FluidUtil.getFluidHandler(conformStack).orElse(null);
        boolean flag = true;
        if (getCompoundTag() != null) {
            flag &= getCompoundTag().equals(conformStack.getTag());
        }
        if (getFluid() != null) {
            flag &= fluidHandler != null && fluidHandler.getFluidInTank(0).getFluid() == getFluid();
        }
        if (!ingredient.isEmpty()){
            boolean i = true;
            for (ItemStack ingredientStack : ingredient.getItems()) {
                i &= !ingredientStack.is(conformStack.getItem());
            }
            flag &= !i;
        }
        return flag;
    }

    @Override
    public boolean matches(Inventory iInventory, Level world) {
        return false;
    }

    @Override
    public ItemStack assemble(Inventory inventory, RegistryAccess registryAccess) {
        return null;
    }


    @Override
    public boolean canCraftInDimensions(int p_43999_, int p_44000_) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE.get();
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    @Override
    public int compareTo(@NotNull WaterLevelAndEffectRecipe o) {
        return o.getPriority() > this.getPriority() ? 1 : -1;
    }


    public static WaterLevelAndEffectRecipe getRecipeFromItem(Level level, ItemStack itemStack) {
        List<WaterLevelAndEffectRecipe> recipes = new ArrayList<>();
        for (WaterLevelAndEffectRecipe recipe : level.getRecipeManager().getAllRecipesFor(TYPE.get())) {
            if (recipe.conform(itemStack)) {
                recipes.add(recipe);
            }
        }
        if (recipes.size() > 0){
            Collections.sort(recipes);
            return recipes.get(0);
        }
        return null;
    }
}

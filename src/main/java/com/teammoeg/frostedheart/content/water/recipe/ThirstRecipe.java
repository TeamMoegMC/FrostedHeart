package com.teammoeg.frostedheart.content.water.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public class ThirstRecipe implements IThirstRecipe {
    protected final int duration, amplifier;
    protected float probability;
    protected final ResourceLocation id;
    protected final String group;
    protected final Ingredient ingredient;
    protected final Fluid fluid;
    protected final CompoundTag compoundTag;

    public static RegistryObject<RecipeSerializer<ThirstRecipe>> SERIALIZER;
    public static RegistryObject<RecipeType<ThirstRecipe>> TYPE;

    public static class Serializer implements RecipeSerializer<ThirstRecipe> {


        @Override
        public ThirstRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            Fluid fluid = null;
            CompoundTag compoundTag = null;
            String group = GsonHelper.getAsString(json, "group", "");
            //fluid
            if (GsonHelper.isValidNode(json ,"fluid")) {
                String fluidName = GsonHelper.getAsString(json, "fluid", "");
                fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluidName));
            }
            //nbt
            if (GsonHelper.isValidNode(json ,"nbt")) {
                JsonObject nbt = GsonHelper.getAsJsonObject(json, "nbt");
                try {
                    compoundTag = NbtUtils.snbtToStructure(nbt.toString());
                } catch (CommandSyntaxException e) {
                    System.out.println(recipeId + ": no nbt.");
                }
            }
            //ingredient
            Ingredient ingredient = Ingredient.EMPTY;
            if (GsonHelper.isValidNode(json,"ingredient")) {
                JsonElement jsonelement = GsonHelper.isArrayNode(json, "ingredient") ? GsonHelper.getAsJsonArray(json, "ingredient") : GsonHelper.getAsJsonObject(json, "ingredient");
                ingredient = Ingredient.fromJson(jsonelement);
            }

            int duration = GsonHelper.getAsInt(json, "duration", 2000);
            int amplifier = GsonHelper.getAsInt(json, "amplifier", 0);
            float probability = GsonHelper.getAsFloat(json, "probability", 0.75f);

            return (ThirstRecipe) new ThirstRecipe(recipeId, group, duration, amplifier, probability, ingredient, fluid, compoundTag);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ThirstRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            recipe.getIngredient().toNetwork(buffer);
            buffer.writeVarInt(recipe.getDuration());
            buffer.writeVarInt(recipe.getAmplifier());
            buffer.writeFloat(recipe.getProbability());
            buffer.writeUtf(recipe.getFluid() == null ? "" :ForgeRegistries.FLUIDS.getKey(recipe.getFluid()).toString());
            buffer.writeNbt(recipe.getCompoundTag());
        }

        @javax.annotation.Nullable
        @Override
        public ThirstRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf byteBuf) {
            String group = byteBuf.readUtf();
            Ingredient ingredient = Ingredient.fromNetwork(byteBuf);
            int duration = byteBuf.readVarInt();
            int amplifier = byteBuf.readVarInt();
            float probability = byteBuf.readFloat();
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(byteBuf.readUtf()));
            CompoundTag compoundTag = byteBuf.readNbt();

            return (ThirstRecipe) new ThirstRecipe(recipeId, group, duration, amplifier, probability, ingredient, fluid, compoundTag);

        }

    }

    public ThirstRecipe( ResourceLocation id, String group, int duration, int amplifier, float probability, Ingredient ingredient, Fluid fluid, CompoundTag compoundTag) {
        super();
        this.duration = duration;
        this.amplifier = amplifier;
        this.probability = probability;
        this.id = id;
        this.group = group;
        this.ingredient = ingredient;
        this.fluid = fluid;
        if (compoundTag != null) compoundTag.remove("palette");
        this.compoundTag = compoundTag;
    }


    public int getAmplifier() {
        return amplifier;
    }

    public int getDuration() {
        return duration;
    }

    public float getProbability() {return probability;}

    public Ingredient getIngredient() {
        return ingredient;
    }

    public CompoundTag getCompoundTag() {
        return compoundTag;
    }

    public Fluid getFluid() {
        return fluid;
    }

    public boolean conform(ItemStack conformStack) {
        IFluidHandler fluidHandler = FluidUtil.getFluidHandler(conformStack).orElse(null);
        boolean flag = true;
        if (getCompoundTag() != null) {
            if (conformStack.getTag() == null) return false;
            for (String key : getCompoundTag().getAllKeys()) {
                flag &= getCompoundTag().get(key).equals(conformStack.getTag().get(key));
            }
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
        return null;
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

    @Override
    public String getGroup() {
        return this.group;
    }

    public static ThirstRecipe getRecipeFromItem(Level world, ItemStack itemStack) {
        List<ThirstRecipe> list = new ArrayList<>();
        if (world != null) {
            list.addAll(world.getRecipeManager().getAllRecipesFor(TYPE.get()));
        }
        for (ThirstRecipe recipe : list) {
            if (recipe.conform(itemStack)) {
                return recipe;
            }
        }
        return null;
    }
}

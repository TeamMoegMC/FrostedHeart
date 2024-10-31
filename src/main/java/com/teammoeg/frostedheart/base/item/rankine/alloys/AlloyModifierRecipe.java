package com.teammoeg.frostedheart.base.item.rankine.alloys;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.teammoeg.frostedheart.base.item.rankine.init.RankineRecipeSerializers;
import com.teammoeg.frostedheart.base.item.rankine.init.RankineRecipeTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlloyModifierRecipe implements Recipe<Container> {

    protected Ingredient ingredient;
    protected final ResourceLocation id;
    private final List<AlloyModifier> modifiers;
    private final List<String> enchantments;
    private final List<String> enchantmentTypes;
    public AlloyModifierRecipe(ResourceLocation id, Ingredient input, List<AlloyModifier> alloyModifiers, List<String> enchantmentsIn, List<String> enchantmentTypesIn) {
        this.id = id;
        this.ingredient = input;
        this.modifiers = alloyModifiers;
        this.enchantments = enchantmentsIn;
        this.enchantmentTypes = enchantmentTypesIn;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public String getGroup() {
        return "";
    }

    @Override
    public boolean matches(Container inv, Level worldIn) {
        return this.ingredient.test(inv.getItem(0));
    }

    @Override
    public ItemStack assemble(Container inv, RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.withSize(1,ingredient);
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    public List<AlloyModifier> getModifiers() {
        return modifiers;
    }

    public List<String> getEnchantments() {
        return enchantments;
    }

    public List<String> getEnchantmentTypes() {
        return enchantmentTypes;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RankineRecipeSerializers.ALLOY_MODIFIER_RECIPE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RankineRecipeTypes.ALLOY_MODIFIER.get();
    }

    public static ItemStack deserializeBlock(JsonObject object) {
        String s = GsonHelper.getAsString(object, "block");

        Block block = BuiltInRegistries.BLOCK.getOptional(new ResourceLocation(s)).orElseThrow(() -> {
            return new JsonParseException("Unknown block '" + s + "'");
        });

        if (object.has("data")) {
            throw new JsonParseException("Disallowed data tag found");
        } else {
            return BlockRecipeHelper.getBlockItemStack(object);
        }
    }

    public static class Serializer implements RecipeSerializer<AlloyModifierRecipe> {

        @Override
        public AlloyModifierRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            String[] s = recipeId.getPath().split("/");
            String nm = recipeId.getNamespace() + ":" + s[s.length-1];
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "input"));

            List<AlloyModifier> alloyModifiers = new ArrayList<>();
            if (json.has("modifiers")) {
                JsonArray modTypes = GsonHelper.getAsJsonArray(json,"modifiers");
                JsonArray modConds = GsonHelper.getAsJsonArray(json,"modifierTypes");
                JsonArray modVals = GsonHelper.getAsJsonArray(json,"values");
                for (int i = 0; i < modTypes.size(); i++) {
                    String inName = nm + "_" + modTypes.get(i).getAsString().toLowerCase(Locale.ROOT);
                    alloyModifiers.add(new AlloyModifier(inName,modTypes.get(i).getAsString().toUpperCase(Locale.ROOT),modConds.get(i).getAsString().toUpperCase(Locale.ROOT),modVals.get(i).getAsFloat()));
                }
            }
            List<String> enchantments = new ArrayList<>();
            List<String> enchantmentTypes = new ArrayList<>();

            if (json.has("enchantments")) {
                JsonArray e = GsonHelper.getAsJsonArray(json,"enchantments");
                JsonArray eTypes = GsonHelper.getAsJsonArray(json,"enchantmentTypes");
                for (int i = 0; i < e.size(); i++) {
                    enchantments.add(e.get(i).getAsString().toLowerCase(Locale.ROOT));
                    enchantmentTypes.add(eTypes.get(i).getAsString().toUpperCase(Locale.ROOT));
                }
            }
            return new AlloyModifierRecipe(recipeId,ingredient,alloyModifiers,enchantments,enchantmentTypes);
        }

        @Nullable
        @Override
        public AlloyModifierRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            String[] s = recipeId.getPath().split("/");
            String nm = recipeId.getNamespace() + ":" + s[s.length-1];
            Ingredient input = Ingredient.fromNetwork(buffer);

            int size = buffer.readInt();
            List<AlloyModifier> modifiers = new ArrayList<>();
            for (int j = 0; j < size; j++) {
                String type = buffer.readUtf().toUpperCase(Locale.ROOT);
                String cond = buffer.readUtf().toUpperCase(Locale.ROOT);
                float val = buffer.readFloat();
                String inName = nm + "_" + type.toLowerCase(Locale.ROOT);
                modifiers.add(new AlloyModifier(inName,type,cond,val));
            }

            size = buffer.readInt();
            List<String> enchantments = new ArrayList<>();
            List<String> enchantmentTypes = new ArrayList<>();
            for (int j = 0; j < size; j++) {
                enchantments.add(buffer.readUtf().toLowerCase(Locale.ROOT));
                enchantmentTypes.add(buffer.readUtf().toUpperCase(Locale.ROOT));
            }

            return new AlloyModifierRecipe(recipeId,input,modifiers,enchantments,enchantmentTypes);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, AlloyModifierRecipe recipe) {
            recipe.getIngredient().toNetwork(buffer);

            int size = recipe.getModifiers().size();
            buffer.writeInt(size);
            for (int i = 0; i < size; i++) {
                AlloyModifier mod = recipe.getModifiers().get(i);
                buffer.writeUtf(mod.getType().toString().toUpperCase(Locale.ROOT));
                buffer.writeUtf(mod.getCondition().toString().toUpperCase(Locale.ROOT));
                buffer.writeFloat(mod.getValue());
            }

            size = recipe.getEnchantments().size();
            buffer.writeInt(size);
            for (int i = 0; i < size; i++) {
                buffer.writeUtf(recipe.getEnchantments().get(i));
                buffer.writeUtf(recipe.getEnchantmentTypes().get(i));
            }
        }
    }

}

package com.teammoeg.frostedheart.content.nutrition.recipe;

import com.google.gson.JsonObject;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class NutritionRecipe implements Recipe<Inventory> {
    public final float fat,carbohydrate,protein,vegetable;
    protected final ResourceLocation id;
    protected final String group;
    protected final Ingredient ingredient;

    public static RegistryObject<RecipeSerializer<NutritionRecipe>> SERIALIZER;
    public static RegistryObject<RecipeType<NutritionRecipe>> TYPE;

    public static class Builder implements RecipeBuilder{
        private float fat,carbohydrate,protein,vegetable;
        protected Item item;
        private final Advancement.Builder advancement = Advancement.Builder.recipeAdvancement();

        public Builder() {
        }

        public Builder item(Item item) {
            this.item = item;
            return this;
        }

        public Builder nutrition(float fat, float carbohydrate, float protein, float vegetable) {
            this.fat = fat;
            this.carbohydrate = carbohydrate;
            this.protein = protein;
            this.vegetable = vegetable;
            return this;
        }

        public Builder fat(float fat) {
            this.fat = fat;
            return this;
        }

        public Builder carbohydrate(float carbohydrate) {
            this.carbohydrate = carbohydrate;
            return this;
        }

        public Builder protein(float protein) {
            this.protein = protein;
            return this;
        }

        public Builder vegetable(float vegetable) {
            this.vegetable = vegetable;
            return this;
        }

        @Override
        public Builder unlockedBy(String s, CriterionTriggerInstance criterionTriggerInstance) {
            this.advancement.addCriterion(s, criterionTriggerInstance);
            return this;
        }

        @Override
        public Builder group(@Nullable String s) {
            return null;
        }

        @Override
        public Item getResult() {
            return item;
        }

        @Override
        public void save(Consumer<FinishedRecipe> pFinishedRecipeConsumer, ResourceLocation pRecipeId) {
            this.advancement.parent(ROOT_RECIPE_ADVANCEMENT).addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pRecipeId)).rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(pRecipeId)).requirements(RequirementsStrategy.OR);
            pFinishedRecipeConsumer.accept(new Result(pRecipeId,fat,carbohydrate,protein,vegetable,item, this.advancement, pRecipeId.withPrefix("recipes/diet_value/")));
        }
        public static class Result implements FinishedRecipe {

            private float fat,carbohydrate,protein,vegetable;
            protected Item item;
            private final ResourceLocation id;
            private final Advancement.Builder advancement;
            private final ResourceLocation advancementId;

            public Result(ResourceLocation id,float fat,float carbohydrate,float protein,float vegetable, Item item,Advancement.Builder advancement, ResourceLocation advancementId) {
                this.id = id;
                this.advancement = advancement;
                this.advancementId = advancementId;
                this.fat = fat;
                this.carbohydrate = carbohydrate;
                this.protein = protein;
                this.vegetable = vegetable;
                this.item = item;
            }

            @Override
            public void serializeRecipeData(JsonObject json) {
                JsonObject group = new JsonObject();
                group.addProperty("fat", this.fat);
                group.addProperty("carbohydrate", this.carbohydrate);
                group.addProperty("protein", this.protein);
                group.addProperty("vegetable", this.vegetable);
                json.add("group", group);
                json.addProperty("item", ForgeRegistries.ITEMS.getKey(this.item).toString());
            }

            @Override
            public ResourceLocation getId() {
                return id;
            }

            @Override
            public RecipeSerializer<?> getType() {
                return NutritionRecipe.SERIALIZER.get();
            }

            @Nullable
            @Override
            public JsonObject serializeAdvancement() {
                return this.advancement.serializeToJson();
            }

            @Nullable
            @Override
            public ResourceLocation getAdvancementId() {
                return this.advancementId;
            }
        }
    }


    public static class Serializer implements RecipeSerializer<NutritionRecipe> {


        @Override
        public NutritionRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            String group = GsonHelper.getAsString(json, "group", "");
            Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.tryParse(GsonHelper.getAsString(json, "item", "")));
            float fat = GsonHelper.getAsFloat(json, "fat");
            float carbohydrate = GsonHelper.getAsFloat(json, "carbohydrate");
            float protein = GsonHelper.getAsFloat(json, "protein");
            float vegetable = GsonHelper.getAsFloat(json, "vegetable");
            return new NutritionRecipe(recipeId, group,fat,carbohydrate,protein,vegetable,Ingredient.of(item));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, NutritionRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            recipe.getIngredient().toNetwork(buffer);
            buffer.writeFloat(recipe.fat);
            buffer.writeFloat(recipe.carbohydrate);
            buffer.writeFloat(recipe.protein);
            buffer.writeFloat(recipe.vegetable);
        }

        @javax.annotation.Nullable
        @Override
        public NutritionRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf byteBuf) {
            String group = byteBuf.readUtf();
            Ingredient ingredient = Ingredient.fromNetwork(byteBuf);
            float fat = byteBuf.readFloat();
            float carbohydrate = byteBuf.readFloat();
            float protein = byteBuf.readFloat();
            float vegetable = byteBuf.readFloat();
            return new NutritionRecipe(recipeId, group,fat,carbohydrate,protein,vegetable,  ingredient);
        }

    }

    public NutritionRecipe(ResourceLocation id, String group, float fat, float carbohydrate, float protein, float vegetable, Ingredient ingredient) {
        super();
        this.fat = fat;
        this.carbohydrate = carbohydrate;
        this.protein = protein;
        this.vegetable = vegetable;
        this.id = id;
        this.group = group;
        this.ingredient = ingredient;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }


    public boolean conform(ItemStack conformStack) {
        return ingredient.test(conformStack);
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

    public static NutritionRecipe getRecipeFromItem(Level world, ItemStack itemStack) {
        List<NutritionRecipe> list = new ArrayList<>();
        if (world != null) {
            list.addAll(world.getRecipeManager().getAllRecipesFor(TYPE.get()));
        }
        for (NutritionRecipe recipe : list) {
            if (recipe.conform(itemStack)) {
                return recipe;
            }
        }
        return null;
    }
}

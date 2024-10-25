package com.teammoeg.frostedheart.content.nutrition.recipe;

import com.google.gson.JsonElement;
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
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

public class NutritionRecipe implements Recipe<Inventory> {
    public final float vitamin,carbohydrate,protein,vegetable;
    protected final ResourceLocation id;
    protected final String group;
    protected final Ingredient ingredient;

    public static RegistryObject<RecipeSerializer<NutritionRecipe>> SERIALIZER;
    public static RegistryObject<RecipeType<NutritionRecipe>> TYPE;

    public static class Builder implements RecipeBuilder{
        private float vitamin,carbohydrate,protein,vegetable;
        protected Ingredient ingredient;
        @Nullable
        private String group;
        private final Advancement.Builder advancement = Advancement.Builder.recipeAdvancement();

        public Builder() {
        }

        public Builder item(Ingredient ingredient) {
            this.ingredient = ingredient;
            return this;
        }

        public Builder item(ItemStack itemStack) {
            return item(Ingredient.of(itemStack));
        }

        public Builder item(Item item) {
            return item(Ingredient.of(item));
        }

        public Builder item(TagKey<Item> item) {
            return item(Ingredient.of(item));
        }

        public Builder nutrition(float vitamin, float carbohydrate, float protein, float vegetable) {
            this.vitamin = vitamin;
            this.carbohydrate = carbohydrate;
            this.protein = protein;
            this.vegetable = vegetable;
            return this;
        }

        public Builder vitamin(float vitamin) {
            this.vitamin = vitamin;
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
            this.group = s;
            return this;
        }

        @Override
        public Item getResult() {
            return ingredient.getItems()[0].getItem();
        }

        @Override
        public void save(Consumer<FinishedRecipe> pFinishedRecipeConsumer, ResourceLocation pRecipeId) {
            this.advancement.parent(ROOT_RECIPE_ADVANCEMENT).addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pRecipeId)).rewards(net.minecraft.advancements.AdvancementRewards.Builder.recipe(pRecipeId)).requirements(RequirementsStrategy.OR);
            pFinishedRecipeConsumer.accept(new Result(pRecipeId,vitamin,carbohydrate,protein,vegetable,ingredient, this.group == null ? "" : this.group, this.advancement, pRecipeId.withPrefix("recipes/nutrition/")));
        }
        public static class Result implements FinishedRecipe {

            private float vitamin,carbohydrate,protein,vegetable;
            protected Ingredient ingredient;
            @Nullable
            private String group;
            private final ResourceLocation id;
            private final Advancement.Builder advancement;
            private final ResourceLocation advancementId;

            public Result(ResourceLocation id,float vitamin,float carbohydrate,float protein,float vegetable, Ingredient ingredient,String group,Advancement.Builder advancement, ResourceLocation advancementId) {
                this.id = id;
                this.advancement = advancement;
                this.advancementId = advancementId;
                this.vitamin = vitamin;
                this.carbohydrate = carbohydrate;
                this.protein = protein;
                this.vegetable = vegetable;
                this.ingredient = ingredient;
                this.group = group;
            }

            @Override
            public void serializeRecipeData(JsonObject json) {
                json.addProperty("group", this.group);
                json.addProperty("vitamin", this.vitamin);
                json.addProperty("carbohydrate", this.carbohydrate);
                json.addProperty("protein", this.protein);
                json.addProperty("vegetable", this.vegetable);
                json.add("ingredient", this.ingredient.toJson());
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
            //ingredient
            Ingredient ingredient = Ingredient.EMPTY;
            if (GsonHelper.isValidNode(json,"ingredient")) {
                JsonElement jsonelement = GsonHelper.isArrayNode(json, "ingredient") ? GsonHelper.getAsJsonArray(json, "ingredient") : GsonHelper.getAsJsonObject(json, "ingredient");
                ingredient = Ingredient.fromJson(jsonelement);
            }

            //private final float vitamin,carbohydrate,protein,vegetable;
            float vitamin = GsonHelper.getAsFloat(json, "vitamin");
            float carbohydrate = GsonHelper.getAsFloat(json, "carbohydrate");
            float protein = GsonHelper.getAsFloat(json, "protein");
            float vegetable = GsonHelper.getAsFloat(json, "vegetable");

            return new NutritionRecipe(recipeId, group,vitamin,carbohydrate,protein,vegetable, ingredient);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, NutritionRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            recipe.getIngredient().toNetwork(buffer);
            buffer.writeFloat(recipe.vitamin);
            buffer.writeFloat(recipe.carbohydrate);
            buffer.writeFloat(recipe.protein);
            buffer.writeFloat(recipe.vegetable);
        }

        @javax.annotation.Nullable
        @Override
        public NutritionRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf byteBuf) {
            String group = byteBuf.readUtf();
            Ingredient ingredient = Ingredient.fromNetwork(byteBuf);
            float vitamin = byteBuf.readFloat();
            float carbohydrate = byteBuf.readFloat();
            float protein = byteBuf.readFloat();
            float vegetable = byteBuf.readFloat();
            return new NutritionRecipe(recipeId, group,vitamin,carbohydrate,protein,vegetable,  ingredient);
        }

    }

    public NutritionRecipe(ResourceLocation id, String group, float vitamin, float carbohydrate, float protein, float vegetable, Ingredient ingredient) {
        super();
        this.vitamin = vitamin;
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

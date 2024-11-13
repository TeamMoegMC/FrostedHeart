package com.teammoeg.frostedheart.base.item.rankine.alloys;

import com.teammoeg.frostedheart.base.item.rankine.init.RankineRecipeTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public interface IAlloyItem {

    default void createAlloyNBT(ItemStack stack, Level worldIn, Map<ElementRecipe,Integer> elementMap, @Nullable ResourceLocation alloyRecipe, @Nullable String nameOverride) {
        if (stack.getTag() != null && stack.getTag().getBoolean("RegenerateAlloy")) {
            stack.getTag().remove("RegenerateAlloy");
        }

        CompoundTag listnbt = new CompoundTag();
        ListTag elements = new ListTag();
        for (Map.Entry<ElementRecipe, Integer> entry : elementMap.entrySet()) {
            int perc = entry.getValue();
            CompoundTag compoundnbt = new CompoundTag();
            compoundnbt.putString("id", String.valueOf(entry.getKey().getId()));
            compoundnbt.putShort("percent", (short)perc);
            elements.add(compoundnbt);
        }
        listnbt.putString("comp", AlloyRecipeHelper.getDirectComposition(elementMap));
        if (alloyRecipe != null) {
            listnbt.putString("recipe",alloyRecipe.toString());
        }
        stack.getOrCreateTag().put("StoredAlloy", listnbt);
        stack.getOrCreateTag().put("Elements",elements);

        if (nameOverride != null && stack.getTag() != null) {
            stack.getTag().putString("nameOverride",nameOverride);
        }
    }

    default void createAlloyNBT(ItemStack stack, Level worldIn, String composition, @Nullable ResourceLocation alloyRecipe, @Nullable String nameOverride) {
        createAlloyNBT(stack,worldIn,getElementMap(composition,worldIn),alloyRecipe,nameOverride);
    }

    static void createDirectAlloyNBT(ItemStack stack, @Nullable String composition, @Nullable String alloyRecipe, @Nullable String nameOverride) {
        createDirectAlloyNBT(stack, composition, alloyRecipe, nameOverride,true);
    }

    static void createDirectAlloyNBT(ItemStack stack, @Nullable String composition, @Nullable String alloyRecipe, @Nullable String nameOverride,boolean regenerate) {
        if (regenerate) {
            stack.getOrCreateTag().putBoolean("RegenerateAlloy",true);
        }
        CompoundTag listnbt = new CompoundTag();
        if (composition != null) {
            listnbt.putString("comp",composition);
        }
        if (alloyRecipe != null) {
            listnbt.putString("recipe",alloyRecipe);
        }
        getAlloyNBT(stack).add(listnbt);
        stack.getOrCreateTag().put("StoredAlloy",listnbt);

        if (nameOverride != null) {
            stack.getOrCreateTag().putString("nameOverride",nameOverride);
        }
    }

    static void addColorNBT(ItemStack stack, int color) {
        stack.getOrCreateTag().putInt("color",color);
    }

    static int getColorNBT(ItemStack stack) {
        if (stack.getTag() != null && stack.getTag().contains("color")) {
            return stack.getTag().getInt("color");
        }
        return 16777215;
    }

    static ListTag getAlloyNBT(ItemStack stack) {
        CompoundTag compoundnbt = stack.getTag();
        return compoundnbt != null ? compoundnbt.getList("StoredAlloy", 10) : new ListTag();
    }

    default String getAlloyCompositionString(ItemStack stack) {
        return stack.getTag() != null ? stack.getTag().getCompound("StoredAlloy").getString("comp") : "";
    }

    default String getAlloyRecipeString(ItemStack stack) {
        return stack.getTag() != null ? stack.getTag().getCompound("StoredAlloy").getString("recipe") : "";
    }

    static ListTag getElementNBT(ItemStack stack) {
        CompoundTag compoundnbt = stack.getTag();
        return compoundnbt != null ? compoundnbt.getList("Elements", 10) : new ListTag();
    }

    default boolean isAlloyInit(ItemStack stack) {
        return stack.getTag() != null && !stack.getTag().getCompound("StoredAlloy").isEmpty();
    }

    default boolean needsRefresh(ItemStack stack) {
        return stack.getTag() != null && !stack.getTag().getCompound("StoredAlloy").isEmpty() && stack.getTag().getBoolean("RegenerateAlloy");
    }

    default void setRefresh(ItemStack stack) {
        if (stack.getTag() != null && !stack.getTag().getCompound("StoredAlloy").isEmpty()) {
            stack.getTag().putBoolean("RegenerateAlloy",true);
        }
    }

    default boolean checkCompositionRequirement(ItemStack stack, Level worldIn, ElementRecipe element, String operand, int percentage) {
        Map<ElementRecipe,Integer> elementMap = this.getElementMap(getAlloyComposition(stack),worldIn);
        if (elementMap.containsKey(element)) {
            if (percentage == -1) {
                switch (operand){
                    case "==":
                    case "=":
                        return true;
                    case "!=":
                        return false;
                }
            } else {
                switch (operand) {
                    case "==":
                    case "=":
                        return elementMap.get(element) == percentage;
                    case ">=":
                        return elementMap.get(element) >= percentage;
                    case "<=":
                        return elementMap.get(element) <= percentage;
                    case "!=":
                        return elementMap.get(element) != percentage;
                }
            }
        }
        return false;
    }

    static String getAlloyComposition(ItemStack stack)
    {
        if (stack.getTag() != null) {
            return stack.getTag().getCompound("StoredAlloy").getString("comp");
        } else {
            return "";
        }
    }

    static String getNameOverride(ItemStack stack)
    {
        if (stack.getTag() != null) {
            return stack.getTag().getString("nameOverride");
        } else {
            return "";
        }
    }

    static ResourceLocation getAlloyRecipe(ItemStack stack)
    {
        if (stack.getTag() != null && !stack.getTag().getCompound("StoredAlloy").getString("recipe").isEmpty()) {
            return new ResourceLocation(stack.getTag().getCompound("StoredAlloy").getString("recipe"));
        } else {
            return null;
        }
    }

    static ListTag getAlloyModifiers(ItemStack stack)
    {
        CompoundTag compoundnbt = stack.getTag();
        return compoundnbt != null ? compoundnbt.getList("AlloyModifiers", 10) : new ListTag();
    }

    default List<ElementRecipe> getElementRecipes(String c, @Nullable Level worldIn) {
        if (worldIn != null) {
            if (c.contains("-")) {
                String[] comp = c.split("-");
                List<ElementRecipe> list = new ArrayList<>();
                for (String e: comp)
                {
                    String str = e.replaceAll("[^A-Za-z]+", "");
                    worldIn.getRecipeManager().getAllRecipesFor(RankineRecipeTypes.ELEMENT.get()).stream().filter(elementRecipe -> elementRecipe.getSymbol().equals(str)).findFirst().ifPresent(list::add);
                }
                return list;
            }
            return Collections.emptyList();

        } else {
            return Collections.emptyList();
        }

    }

    default List<Integer> getPercents(String c)
    {
        if (c.contains("-")) {
            String[] comp = c.split("-");
            List<Integer> list = new ArrayList<>();
            for (String e: comp)
            {
                String str = e.replaceAll("\\D+", "");
                list.add(Integer.parseInt(str));
            }
            return list;
        }
        return Collections.emptyList();
    }

    default Map<ElementRecipe,Integer> getElementMap(String c, @Nullable Level worldIn) {
        List<ElementRecipe> elementRecipes = getElementRecipes(c,worldIn);
        List<Integer> percents = getPercents(c);
        Map<ElementRecipe,Integer> elementMap = new HashMap<>();
        for (int i = 0; i < elementRecipes.size(); i++) {
            if (i < percents.size()) {
                elementMap.put(elementRecipes.get(i),percents.get(i));
            }
        }
        for (Map.Entry<ElementRecipe, Integer> entry : elementMap.entrySet()) {
            int perc = entry.getValue();
            CompoundTag compoundnbt = new CompoundTag();
            compoundnbt.putString("id", String.valueOf(entry.getKey().getId()));
            compoundnbt.putShort("percent", (short)perc);
        }
        return elementMap;
    }

    default OldAlloyingRecipe getAlloyingRecipe(ResourceLocation rs, Level worldIn) {
        if (rs != null) {
            Optional<? extends Recipe<?>> opt = worldIn.getRecipeManager().byKey(rs);
            if (opt.isPresent() && opt.get() instanceof OldAlloyingRecipe) {
                return (OldAlloyingRecipe) opt.get();
            }
        }
        return null;
    }

    @Nonnull
    static String getSubtype(ItemStack stack) {
        return stack.hasTag() ? IAlloyItem.getNameOverride(stack).toLowerCase(Locale.ROOT).replace(" ","_") : "";
    }

    default void addAlloyInformation(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if (this.isAlloyInit(stack)) {
            if (IAlloyItem.getAlloyComposition(stack).isEmpty()) {
                tooltip.add((Component.literal("Any Composition").withStyle(ChatFormatting.GOLD)));
            } else {
                tooltip.add((Component.literal("Composition: " + IAlloyItem.getAlloyComposition(stack)).withStyle(ChatFormatting.GOLD)));
            }

            if (!IAlloyItem.getAlloyModifiers(stack).isEmpty()) {
                tooltip.add((Component.literal("Modifier: " + (IAlloyItem.getAlloyModifiers(stack).getCompound(0).getString("modifierName"))).withStyle(ChatFormatting.AQUA)));
            }
        }
    }

    default void addAdvancedAlloyInformation(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if (IAlloyItem.getAlloyRecipe(stack) != null) {
            tooltip.add((Component.literal("Recipe: " + (IAlloyItem.getAlloyRecipe(stack))).withStyle(ChatFormatting.LIGHT_PURPLE)));
        } else {
            tooltip.add((Component.literal("No Recipe Defined").withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
    }

    default String generateLangFromRecipe(ResourceLocation recipe) {
        if (recipe == null) {
            return "item.rankine.custom_alloy_default";
        } else {
            String[] s = recipe.getPath().split("/");
            return "item." + recipe.getNamespace() + "." + s[s.length-1];
        }
    }


    String getDefaultComposition();

    ResourceLocation getDefaultRecipe();
}

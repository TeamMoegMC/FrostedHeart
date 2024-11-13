package com.teammoeg.frostedheart.base.item.rankine.alloys;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class AlloyIngredientHelper {

    public static Ingredient deserialize(@Nullable JsonElement json, @Nullable String alloyComp, @Nullable String alloyRecipe, @Nullable String name) {
        if (json != null && !json.isJsonNull()) {
            Ingredient ret = net.minecraftforge.common.crafting.CraftingHelper.getIngredient(json,true);
            if (alloyRecipe != null)
            {
                List<ItemStack> stacks = new ArrayList<>();
                for (ItemStack s : ret.getItems())
                {
                    IAlloyItem.createDirectAlloyNBT(s,alloyComp,alloyRecipe,name);
                    stacks.add(s);
                }
                ret = Ingredient.of(stacks.toArray(new ItemStack[0]));
            }

            return ret;
        } else {
            throw new JsonSyntaxException("Item cannot be null");
        }
    }

    public static Ingredient deserialize(@Nullable JsonElement json, @Nullable String alloyComp, @Nullable String alloyRecipe, @Nullable String name, int color) {
        if (json != null && !json.isJsonNull()) {
            Ingredient ret = net.minecraftforge.common.crafting.CraftingHelper.getIngredient(json, true);
            if (alloyComp != null || alloyRecipe != null || name != null || color != 16777215)
            {
                List<ItemStack> stacks = new ArrayList<>();
                for (ItemStack s : ret.getItems())
                {
                    IAlloyItem.createDirectAlloyNBT(s,alloyComp,alloyRecipe,name);
                    IAlloyItem.addColorNBT(s,color);
                    stacks.add(s);
                }
                ret = Ingredient.of(stacks.toArray(new ItemStack[0]));
            }

            return ret;
        } else {
            throw new JsonSyntaxException("Item cannot be null");
        }
    }



    public static ItemStack getItemStack(JsonObject json, boolean readNBT)
    {
        return getItemStack(json, readNBT, true);
    }

    public static ItemStack getItemStack(JsonObject json, boolean readNBT, boolean includeCount)
    {
        String itemName = GsonHelper.getAsString(json, "item");

        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName));

        if (item == null)
            throw new JsonSyntaxException("Unknown item '" + itemName + "'");


        if (readNBT && json.has("nbt"))
        {
            try
            {
                JsonElement element = json.get("nbt");
                CompoundTag nbt;
                if(element.isJsonObject())
                    nbt = TagParser.parseTag(new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(element));
                else
                    nbt = TagParser.parseTag(GsonHelper.convertToString(element, "nbt"));

                CompoundTag tmp = new CompoundTag();
                if (nbt.contains("ForgeCaps"))
                {
                    tmp.put("ForgeCaps", nbt.get("ForgeCaps"));
                    nbt.remove("ForgeCaps");
                }

                tmp.put("tag", nbt);
                tmp.putString("id", itemName);
                if (includeCount) {
                    tmp.putInt("Count", GsonHelper.getAsInt(json, "count", 1));
                } else {
                    tmp.putInt("Count", 1);
                }
                return ItemStack.of(tmp);
            }
            catch (CommandSyntaxException e)
            {
                throw new JsonSyntaxException("Invalid NBT Entry: " + e.toString());
            }
        }

        ItemStack ret = new ItemStack(item, GsonHelper.getAsInt(json, "count", 1));
        if (json.has("alloyComp") || json.has("alloyRecipe"))
        {

            String alloyComp = json.has("alloyComp") ? GsonHelper.getAsString(json, "alloyComp") : "";
            String alloyRecipe = json.has("alloyRecipe") ? GsonHelper.getAsString(json, "alloyRecipe") : "";
            //System.out.println("AlloyData detected in recipe!: " + JSONUtils.getString(json, "alloyData"));
            IAlloyItem.createDirectAlloyNBT(ret,alloyComp,alloyRecipe,null,ret.getItem() instanceof IAlloyTool);
        }
        return ret;
    }

}

/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.frostedheart.content.health.tooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.content.climate.food.FoodTemperatureHandler;
import com.teammoeg.frostedheart.content.health.capability.Nutrition;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.util.Lang;
import com.teammoeg.frostedheart.util.client.FineProgressBarBuilder;
import com.teammoeg.frostedheart.util.client.KeyControlledDesc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public class FoodNutritionStats implements TooltipModifier {
    protected final Item item;
    public FoodNutritionStats(Item item) {
        this.item = item;
    }

    @Nullable
    public static FoodNutritionStats create(Item item) {
        return new FoodNutritionStats(item);
    }

    @Override
    public void modify(ItemTooltipEvent context) {
    	final ItemStack stack=context.getItemStack();
    	final Player player=context.getEntity();
        List<Component> stats = getFoodStats(stack, player);
        
        if (FoodTemperatureHandler.isFoodOrDrink(stack) && stats != null && !stats.isEmpty()) {
        	KeyControlledDesc desc = new KeyControlledDesc(()->stats,
                    GLFW.GLFW_KEY_N,
                    "N", 
                    "holdForNutrition"
                    );
            List<Component> tooltip = context.getToolTip();
            tooltip.add(Components.immutableEmpty());
            tooltip.addAll(desc.getCurrentLines());
        }
    }
    static final int FAT_COLOR=0xFFd41c53;
    static final int PROTEIN_COLOR=0xFFd4a31c;
    static final int CARBOHYDRATE_COLOR=0xFFd4781c;
    static final int VEGETABLE_COLOR=0xFF31d41c;
    static final int PROGRESS_LENGTH=100;
    public static List<Component> getFoodStats(ItemStack stack, Player player) {
        

        if(player == null) return null;
        
        Nutrition foodNutrition = NutritionCapability.getFoodNutrition(player, stack);

        if(foodNutrition!=null) {
        	List<Component> list = new ArrayList<>();
            Lang.translate("tooltip", "nutrition")
                    .style(ChatFormatting.GRAY)
                    .addTo(list);
            Nutrition nutrition = foodNutrition.mutableCopy().scale(1/foodNutrition.getNutritionValue());
            FineProgressBarBuilder builder=new FineProgressBarBuilder(PROGRESS_LENGTH);
            //list.add(Lang.str("\uF504").withStyle(FHTextIcon.applyFont(Style.EMPTY)));
            if(nutrition.getFat()>0) {
            	builder.addElement(FAT_COLOR, "\uF504",nutrition.getFat());
            }
            if(nutrition.getProtein()>0) {
            	builder.addElement(PROTEIN_COLOR, "\uF505",nutrition.getProtein());
            }
            if(nutrition.getCarbohydrate()>0) {
            	builder.addElement(CARBOHYDRATE_COLOR, "\uF502",nutrition.getCarbohydrate());
            }
            if(nutrition.getVegetable()>0) {
            	builder.addElement(VEGETABLE_COLOR, "\uF503",nutrition.getVegetable());
            }
            list.add(builder.build());
            list.add(Lang.gui("nutrition.max_level").component());
            double gainLostRate=FHConfig.SERVER.NUTRITION.nutritionGainRate.get()/FHConfig.SERVER.NUTRITION.nutritionConsumptionRate.get()/10000;
            if(foodNutrition.getFat()>0)
            	list.add(Lang.gui("nutrition.fat").color(FAT_COLOR).space().percentage().number(foodNutrition.getFat()*gainLostRate).withStyle(ChatFormatting.GREEN).component());
            if(foodNutrition.getProtein()>0)
            	list.add(Lang.gui("nutrition.protein").color(PROTEIN_COLOR).space().percentage().number(foodNutrition.getProtein()*gainLostRate).withStyle(ChatFormatting.GREEN).component());
            if(foodNutrition.getCarbohydrate()>0)
            	list.add(Lang.gui("nutrition.carbohydrate").color(CARBOHYDRATE_COLOR).space().percentage().number(foodNutrition.getCarbohydrate()*gainLostRate).withStyle(ChatFormatting.GREEN).component());
            if(foodNutrition.getVegetable()>0)
            	list.add(Lang.gui("nutrition.vegetable").color(VEGETABLE_COLOR).space().percentage().number(foodNutrition.getVegetable()*gainLostRate).withStyle(ChatFormatting.GREEN).component());
            return list;
        }
        return null;
    }
//
//    private static void addLine(List<Component> list,String suffix,float value,int color) {
//
//        int progress = Mth.ceil(Mth.clamp(value * 3, 0, 3));
//
//        LangBuilder builder = Lang.translate("tooltip", "nutrition."+suffix)
//                .add(Lang.text(" " + TooltipHelper.makeProgressBar(3, progress))
//                        .style(Style.EMPTY.withColor(color)));
//        builder.addTo(list);
//    }
}

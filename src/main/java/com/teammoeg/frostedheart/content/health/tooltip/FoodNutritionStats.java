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

package com.teammoeg.frostedheart.content.health.tooltip;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.lwjgl.glfw.GLFW;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.teammoeg.chorda.text.Components;
import com.teammoeg.frostedheart.content.climate.food.FoodTemperatureHandler;
import com.teammoeg.frostedheart.content.health.nutrition.FoodNutritionProfile;
import com.teammoeg.frostedheart.content.health.nutrition.FoodNutritionResolver;
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
        
		FoodNutritionProfile foodNutrition = FoodNutritionResolver.resolve(player.level(), stack);

		if(!foodNutrition.isZero()) {
			List<Component> list = new ArrayList<>();
            Lang.translate("tooltip", "nutrition")
                    .style(ChatFormatting.GRAY)
                    .addTo(list);
			float total = foodNutrition.total();
			float fatShare = foodNutrition.fat() / total;
			float proteinShare = foodNutrition.protein() / total;
			float carbohydrateShare = foodNutrition.carbohydrate() / total;
			float vegetableShare = foodNutrition.vegetable() / total;
            FineProgressBarBuilder builder=new FineProgressBarBuilder(PROGRESS_LENGTH);
            //list.add(Lang.str("\uF504").withStyle(FHTextIcon.applyFont(Style.EMPTY)));
			if(fatShare>0) {
				builder.addElement(FAT_COLOR, "\uF504",fatShare);
            }
			if(proteinShare>0) {
				builder.addElement(PROTEIN_COLOR, "\uF505",proteinShare);
            }
			if(carbohydrateShare>0) {
				builder.addElement(CARBOHYDRATE_COLOR, "\uF502",carbohydrateShare);
            }
			if(vegetableShare>0) {
				builder.addElement(VEGETABLE_COLOR, "\uF503",vegetableShare);
            }
            list.add(builder.build());
            list.add(Lang.gui("nutrition.max_level").component());
			if(foodNutrition.fat()>0)
				list.add(Lang.gui("nutrition.fat").color(FAT_COLOR).space().percentage().number(foodNutrition.fat()/100).withStyle(ChatFormatting.GREEN).component());
			if(foodNutrition.protein()>0)
				list.add(Lang.gui("nutrition.protein").color(PROTEIN_COLOR).space().percentage().number(foodNutrition.protein()/100).withStyle(ChatFormatting.GREEN).component());
			if(foodNutrition.carbohydrate()>0)
				list.add(Lang.gui("nutrition.carbohydrate").color(CARBOHYDRATE_COLOR).space().percentage().number(foodNutrition.carbohydrate()/100).withStyle(ChatFormatting.GREEN).component());
			if(foodNutrition.vegetable()>0)
				list.add(Lang.gui("nutrition.vegetable").color(VEGETABLE_COLOR).space().percentage().number(foodNutrition.vegetable()/100).withStyle(ChatFormatting.GREEN).component());
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

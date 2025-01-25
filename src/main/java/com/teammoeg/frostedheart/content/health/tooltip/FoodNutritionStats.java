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

import com.simibubi.create.foundation.item.TooltipModifier;
import com.teammoeg.frostedheart.util.client.KeyControlledDesc;
import com.teammoeg.chorda.lang.Components;
import com.teammoeg.frostedheart.content.climate.food.FoodTemperatureHandler;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;
import com.teammoeg.frostedheart.util.client.FineProgressBarBuilder;
import com.teammoeg.frostedheart.util.client.Lang;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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
        List<Component> stats = getFoodStats(item, context.getItemStack(), context.getEntity());
        KeyControlledDesc desc = new KeyControlledDesc(stats, new ArrayList<>(),
                GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_LEFT_CONTROL,
                "N", "Ctrl",
                "holdForNutrition", "holdForControls"
                );
        if (!stats.isEmpty()) {
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
    public static List<Component> getFoodStats(Item item, ItemStack stack, Player player) {
        List<Component> list = new ArrayList<>();

        if(player == null) return list;
        NutritionCapability.Nutrition foodNutrition = NutritionCapability.getFoodNutrition(player.level(), stack);

        if (FoodTemperatureHandler.isFoodOrDrink(stack) && foodNutrition != null) {
            Lang.translate("tooltip", "nutrition")
                    .style(ChatFormatting.GRAY)
                    .addTo(list);
            NutritionCapability.Nutrition nutrition = foodNutrition.scale(1/foodNutrition.getNutritionValue());
            FineProgressBarBuilder builder=new FineProgressBarBuilder(PROGRESS_LENGTH);
            //list.add(Lang.str("\uF504").withStyle(FHTextIcon.applyFont(Style.EMPTY)));
            if(nutrition.fat()>0) {
            	builder.addElement(FAT_COLOR, "\uF504",nutrition.fat());
            }
            if(nutrition.protein()>0) {
            	builder.addElement(PROTEIN_COLOR, "\uF505",nutrition.protein());
            }
            if(nutrition.carbohydrate()>0) {
            	builder.addElement(CARBOHYDRATE_COLOR, "\uF502",nutrition.carbohydrate());
            }
            if(nutrition.vegetable()>0) {
            	builder.addElement(VEGETABLE_COLOR, "\uF503",nutrition.vegetable());
            }
            list.add(builder.build());
        }
        return list;
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

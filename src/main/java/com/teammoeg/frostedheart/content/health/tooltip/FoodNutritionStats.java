package com.teammoeg.frostedheart.content.health.tooltip;

import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.teammoeg.frostedheart.base.tooltip.KeyControlledDesc;
import com.teammoeg.frostedheart.content.climate.TemperatureDisplayHelper;
import com.teammoeg.frostedheart.content.climate.player.ITempAdjustFood;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.content.health.capability.NutritionCapability;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.infrastructure.data.FHDataManager;
import com.teammoeg.frostedheart.util.lang.Components;
import com.teammoeg.frostedheart.util.lang.FHTextIcon;
import com.teammoeg.frostedheart.util.lang.Lang;
import com.teammoeg.frostedheart.util.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

public class FoodNutritionStats implements TooltipModifier {
    protected final Item item;
    public FoodNutritionStats(Item item) {
        this.item = item;
    }

    @Override
    public void modify(ItemTooltipEvent context) {
        List<Component> stats = getFoodStats(item, context.getItemStack(), context.getEntity());
        KeyControlledDesc desc = new KeyControlledDesc(stats, new ArrayList<>());
        if (!stats.isEmpty()) {
            List<Component> tooltip = context.getToolTip();
            tooltip.add(Components.immutableEmpty());
            tooltip.addAll(desc.getCurrentLines());
        }
    }

    public static List<Component> getFoodStats(Item item, ItemStack stack, Player player) {
        List<Component> list = new ArrayList<>();

        if(player == null) return list;
        NutritionCapability.Nutrition foodNutrition = NutritionCapability.getFoodNutrition(player.level(), stack);

        Lang.translate("tooltip", "nutrition")
                .style(ChatFormatting.GRAY)
                .addTo(list);
        if (foodNutrition != null&&stack.isEdible()) {
            NutritionCapability.Nutrition nutrition = foodNutrition.scale(1/foodNutrition.getNutritionValue());

            if(nutrition.fat()>0) {
                addLine(list,"fat",nutrition.fat(),0xFFd41c53);
            }
            if(nutrition.protein()>0) {
                addLine(list,"protein",nutrition.protein(),0xFFd4a31c);
            }
            if(nutrition.carbohydrate()>0) {
                addLine(list,"carbohydrate",nutrition.carbohydrate(),0xFFd4781c);
            }
            if(nutrition.vegetable()>0) {
                addLine(list,"vegetable",nutrition.vegetable(),0xFF31d41c);
            }

        }
        return list;
    }

    private static void addLine(List<Component> list,String suffix,float value,int color) {

        int progress = Mth.ceil(Mth.clamp(value * 3, 0, 3));

        LangBuilder builder = Lang.translate("tooltip", "nutrition."+suffix)
                .add(Lang.text(" " + TooltipHelper.makeProgressBar(3, progress))
                        .style(Style.EMPTY.withColor(color)));
        builder.addTo(list);
    }
}

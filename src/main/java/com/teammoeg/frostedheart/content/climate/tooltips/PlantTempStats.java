package com.teammoeg.frostedheart.content.climate.tooltips;

import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.teammoeg.frostedheart.content.climate.TemperatureDisplayHelper;
import com.teammoeg.frostedheart.content.climate.WorldTemperature;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.infrastructure.data.FHDataManager;
import com.teammoeg.frostedheart.util.FHTooltipHelper;
import com.teammoeg.frostedheart.util.lang.Components;
import com.teammoeg.frostedheart.util.lang.Lang;
import com.teammoeg.frostedheart.util.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PlantTempStats implements TooltipModifier {
    protected final Block block;
    public PlantTempStats(Block block) {
        this.block = block;
    }

    @Nullable
    public static PlantTempStats create(Item item) {
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            return new PlantTempStats(block);
        }
        return null;
    }

    @Override
    public void modify(ItemTooltipEvent context) {
        List<Component> stats = getStats(block, context.getItemStack(), context.getEntity());
        if (!stats.isEmpty()) {
            List<Component> tooltip = context.getToolTip();
            tooltip.add(Components.immutableEmpty());
            tooltip.addAll(stats);
        }
    }



    public static List<Component> getStats(Block block, ItemStack stack, Player player) {
        List<Component> list = new ArrayList<>();
        PlantTempData data = WorldTemperature.getPlantDataWithDefault(block);
        if (data != null) {
            // maps 0-10 to 0-3
            int barlength = 3;
            float barfactor = 0.1F;

            float minSurviveTemp = (Math.round(data.getMinSurvive() * 10)) / 10.0F;
            float maxSurviveTemp = (Math.round(data.getMaxSurvive() * 10)) / 10.0F;
            Lang.translate("tooltip", "temp.plant.survive").style(ChatFormatting.GRAY).addTo(list);
            int maxProgress = Mth.ceil(Mth.clamp(Math.abs(maxSurviveTemp) * barfactor, 0, barlength));
            int minProgress = Mth.ceil(Mth.clamp(Math.abs(minSurviveTemp) * barfactor, 0, barlength));

            LangBuilder builder = Lang.builder()
                    .add(Lang.text(TemperatureDisplayHelper.toTemperatureFloatString(minSurviveTemp))
                            .style(minSurviveTemp <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD))
                    .add(Lang.text(" - ").style(ChatFormatting.GRAY))
                    .add(Lang.text(TemperatureDisplayHelper.toTemperatureFloatString(maxSurviveTemp))
                            .style(maxSurviveTemp <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD))
                    .add(Lang.text(" "))
                    .add(Lang.text(FHTooltipHelper.makeProgressBarReversed(barlength, minProgress))
                            .style(minSurviveTemp <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD))
                    .add(Lang.text(FHTooltipHelper.makeProgressBar(barlength, maxProgress))
                            .style(maxSurviveTemp <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD));
            builder.addTo(list);

            float minGrowTemp = (Math.round(data.getMinGrow() * 10)) / 10.0F;
            float maxGrowTemp = (Math.round(data.getMaxGrow() * 10)) / 10.0F;
            Lang.translate("tooltip", "temp.plant.grow").style(ChatFormatting.GRAY).addTo(list);
            maxProgress = Mth.ceil(Mth.clamp(Math.abs(maxGrowTemp) * barfactor, 0, barlength));
            minProgress = Mth.ceil(Mth.clamp(Math.abs(minGrowTemp) * barfactor, 0, barlength));
            builder = Lang.builder()
                    .add(Lang.text(TemperatureDisplayHelper.toTemperatureFloatString(minGrowTemp))
                            .style(minGrowTemp <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD))
                    .add(Lang.text(" - ").style(ChatFormatting.GRAY))
                    .add(Lang.text(TemperatureDisplayHelper.toTemperatureFloatString(maxGrowTemp))
                            .style(maxGrowTemp <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD))
                    .add(Lang.text(" "))
                    .add(Lang.text(FHTooltipHelper.makeProgressBarReversed(barlength, minProgress))
                            .style(minGrowTemp <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD))
                    .add(Lang.text(FHTooltipHelper.makeProgressBar(barlength, maxProgress))
                            .style(maxGrowTemp <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD));
            builder.addTo(list);

            float minFertilizeTemp = (Math.round(data.getMinFertilize() * 10)) / 10.0F;
            float maxFertilizeTemp = (Math.round(data.getMaxFertilize() * 10)) / 10.0F;
            Lang.translate("tooltip", "temp.plant.fertilize").style(ChatFormatting.GRAY).addTo(list);
            maxProgress = Mth.ceil(Mth.clamp(Math.abs(maxFertilizeTemp) * barfactor, 0, barlength));
            minProgress = Mth.ceil(Mth.clamp(Math.abs(minFertilizeTemp) * barfactor, 0, barlength));
            builder = Lang.builder()
                    .add(Lang.text(TemperatureDisplayHelper.toTemperatureFloatString(minFertilizeTemp))
                            .style(minFertilizeTemp <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD))
                    .add(Lang.text(" - ").style(ChatFormatting.GRAY))
                    .add(Lang.text(TemperatureDisplayHelper.toTemperatureFloatString(maxFertilizeTemp))
                            .style(maxFertilizeTemp <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD))
                    .add(Lang.text(" "))
                    .add(Lang.text(FHTooltipHelper.makeProgressBarReversed(barlength, minProgress))
                            .style(minFertilizeTemp <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD))
                    .add(Lang.text(FHTooltipHelper.makeProgressBar(barlength, maxProgress))
                            .style(maxFertilizeTemp <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD));
            builder.addTo(list);


            boolean vulnerableSnow = data.isSnowVulnerable();
            if (vulnerableSnow) {
                Lang.translate("tooltip", "temp.plant.snow_vulnerable")
                        .style(ChatFormatting.RED)
                        .addTo(list);
            } else {
                Lang.translate("tooltip", "temp.plant.snow_resistant")
                        .style(ChatFormatting.GREEN)
                        .addTo(list);
            }

            boolean vulnerableBlizzard = data.isBlizzardVulnerable();
            if (vulnerableBlizzard) {
                Lang.translate("tooltip", "temp.plant.blizzard_vulnerable")
                        .style(ChatFormatting.RED)
                        .addTo(list);
            } else {
                Lang.translate("tooltip", "temp.plant.blizzard_resistant")
                        .style(ChatFormatting.GREEN)
                        .addTo(list);
            }

        }
        return list;
    }
}

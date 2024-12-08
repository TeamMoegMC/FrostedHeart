package com.teammoeg.frostedheart.content.climate.tooltips;

import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.teammoeg.frostedheart.content.climate.TemperatureDisplayHelper;
import com.teammoeg.frostedheart.content.climate.data.PlantTempData;
import com.teammoeg.frostedheart.infrastructure.data.FHDataManager;
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
        PlantTempData data = FHDataManager.getPlantData(block);
        if (data != null) {
            float temp = data.getSurvive();
            temp = (Math.round(temp * 10)) / 10.0F;// round
            String s = TemperatureDisplayHelper.toTemperatureDeltaFloatString(temp);
            Lang.translate("tooltip", "temp.plant.survive")
                    .style(ChatFormatting.GRAY)
                    .addTo(list);
            int barlength = 3;
            int progress = Mth.ceil(Mth.clamp(Math.abs(temp) * 0.1, 0, barlength));
            LangBuilder builder = Lang.builder()
                    .add(Lang.text(s + " " + TooltipHelper.makeProgressBar(barlength, progress))
                            .style(temp <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD));
            builder.addTo(list);

            temp = data.getGrow();
            temp = (Math.round(temp * 10)) / 10.0F;// round
            s = TemperatureDisplayHelper.toTemperatureDeltaFloatString(temp);
            Lang.translate("tooltip", "temp.plant.grow")
                    .style(ChatFormatting.GRAY)
                    .addTo(list);
            barlength = 3;
            progress = Mth.ceil(Mth.clamp(Math.abs(temp) * 0.1, 0, barlength));
            builder = Lang.builder()
                    .add(Lang.text(s + " " + TooltipHelper.makeProgressBar(barlength, progress))
                            .style(temp <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD));
            builder.addTo(list);

            temp = data.getBonemeal();
            temp = (Math.round(temp * 10)) / 10.0F;// round
            s = TemperatureDisplayHelper.toTemperatureDeltaFloatString(temp);
            Lang.translate("tooltip", "temp.plant.bonemeal")
                    .style(ChatFormatting.GRAY)
                    .addTo(list);
            barlength = 3;
            progress = Mth.ceil(Mth.clamp(Math.abs(temp) * 0.1, 0, barlength));
            builder = Lang.builder()
                    .add(Lang.text(s + " " + TooltipHelper.makeProgressBar(barlength, progress))
                            .style(temp <= 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD));
            builder.addTo(list);

            boolean vulnerableSnow = data.isSnowVulnerable();
            Lang.translate("tooltip", "temp.plant.snow_vulnerable")
                    .style(vulnerableSnow ? ChatFormatting.RED : ChatFormatting.GREEN)
                    .addTo(list);

            boolean vulnerableBlizzard = data.isBlizzardVulnerable();
            Lang.translate("tooltip", "temp.plant.blizzard_vulnerable")
                    .style(vulnerableBlizzard ? ChatFormatting.RED : ChatFormatting.GREEN)
                    .addTo(list);

        }
        return list;
    }
}

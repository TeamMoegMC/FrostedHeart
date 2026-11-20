package com.teammoeg.frostedheart.content.climate.tooltips;

import java.util.ArrayList;
import java.util.List;
import com.teammoeg.chorda.client.CInputHelper;
import com.teammoeg.chorda.util.TextProgressBarHelper;
import com.teammoeg.frostedheart.bootstrap.common.FHBlocks;
import com.teammoeg.frostedheart.content.climate.network.SoilThermometerUpdatePacket.TreeInfo;
import com.teammoeg.frostedheart.util.Lang;
import com.teammoeg.frostedheart.util.client.FHTextIcon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Crop-style thermometer rows built from a matching server snapshot, never client profile defaults. */
public final class CultivatedTreeTempStats {
    private CultivatedTreeTempStats() {}

    public static List<Component> getStats(@Nullable TreeInfo tree, @Nullable Component fallback,
            BlockState soil, float temperature) {
        List<Component> rows = new ArrayList<>();
        if (tree == null) {
            if (fallback != null) rows.add(Component.translatable("message.frostedheart.cultivated_oak.profile").withStyle(ChatFormatting.YELLOW));
            return rows;
        }
        boolean expanded = CInputHelper.isShiftKeyDown();
        boolean growing = tree.waiting().equals("growing");
        ChatFormatting statusColor = growing ? ChatFormatting.GREEN
                : tree.waiting().equals("profile") ? ChatFormatting.RED : ChatFormatting.YELLOW;
        Lang.translate("tooltip", "temp.plant.survivability.state").style(ChatFormatting.GRAY)
                .add(Component.translatable("message.frostedheart.cultivated_oak." + tree.waiting()).withStyle(statusColor)).addTo(rows);
        rows.add(Component.translatable("message.frostedheart.cultivated_oak.stage." + Mth.clamp(tree.stage(), 0, 4)).withStyle(ChatFormatting.AQUA));
        float ratio = tree.requiredProgress() > 0 ? Mth.clamp(tree.progress() / tree.requiredProgress(), 0, 1) : 0;
        String bar = TextProgressBarHelper.makeProgressBarInterval(8, 0, Math.round(ratio * 8));
        rows.add(Component.translatable("message.frostedheart.cultivated_oak.progress").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" " + bar).withStyle(ChatFormatting.GREEN)));
        ChatFormatting temperatureColor = temperature < tree.idealMin() ? ChatFormatting.AQUA
                : temperature > tree.idealMax() ? ChatFormatting.GOLD : ChatFormatting.GREEN;
        float temperatureRatio = tree.maxTemperature() > tree.minTemperature()
                ? Mth.clamp((temperature - tree.minTemperature()) / (tree.maxTemperature() - tree.minTemperature()), 0, 1) : 0;
        int marker = Math.min(7, (int)(temperatureRatio * 8));
        String temperatureBar = TextProgressBarHelper.makeProgressBarInterval(8, marker, marker + 1);
        Lang.builder().add(FHTextIcon.SOIL_THERMOMETER.getIcon()).add(Lang.text(" "))
                .add(Component.translatable("message.frostedheart.cultivated_oak.current_temperature").withStyle(ChatFormatting.GRAY))
                .add(Lang.text(" " + temperatureBar).style(temperatureColor)).addTo(rows);
        if (expanded) {
            rows.add(Component.empty());
            Lang.translate("tooltip", "temp.plant.grow").style(ChatFormatting.GRAY).addTo(rows);
            PlantTempStats.getTempProgressBar(tree.minTemperature(), tree.maxTemperature(), false).addTo(rows);
            rows.add(Component.translatable("message.frostedheart.cultivated_oak.ideal_temperature").withStyle(ChatFormatting.GRAY));
            PlantTempStats.getTempProgressBar(tree.idealMin(), tree.idealMax(), false).addTo(rows);
        }
        if (expanded || tree.waiting().equals("light")) {
            Lang.translate("tooltip", "temp.plant.light").style(ChatFormatting.GRAY).addTo(rows);
            PlantTempStats.getLightProgressBar(tree.minLight(), 15, false).addTo(rows);
        }
        if (soil.is(FHBlocks.FERTILIZED_DIRT.get())) {
            rows.add(Component.empty());
            if (expanded) Lang.translate("tooltip", "temp.plant.fertilizer").style(ChatFormatting.GRAY).addTo(rows);
            PlantTempStats.getFertilizerStorageBar(soil, false).addTo(rows);
        }
        if (!expanded) {
            rows.add(Component.empty());
            Lang.translate("tooltip", "temp.plant.shift").style(ChatFormatting.GRAY).addTo(rows);
        }
        return rows;
    }
}

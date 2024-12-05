package com.teammoeg.frostedheart.foundation.tooltips;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.utility.Components;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.data.FoodTempData;
import com.teammoeg.frostedheart.content.climate.player.ITempAdjustFood;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.infrastructure.data.FHDataManager;
import com.teammoeg.frostedheart.util.TemperatureDisplayHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * This is only for tooltip available on registry time.
 * ItemStacks are not supported.
 */
public class TemperatureStats implements TooltipModifier {
    protected final Item item;
    protected ItemDescription description;
    public TemperatureStats(Item item) {
        this.item = item;
    }

    @Nullable
    public static TemperatureStats create(Item item) {
        if (FHDataManager.getTempAdjustFood(item) != null) {
            return new TemperatureStats(item);
        }
        return null;
    }

    @Override
    public void modify(ItemTooltipEvent context) {
        List<Component> stats = getTempStats(item, context.getItemStack(), context.getEntity());
        if (!stats.isEmpty()) {
            List<Component> tooltip = context.getToolTip();
            tooltip.add(Components.immutableEmpty());
            tooltip.addAll(stats);
        }
    }

    public static List<Component> getTempStats(Item item, ItemStack stack, Player player) {
        List<Component> list = new ArrayList<>();
        ITempAdjustFood data = FHDataManager.getTempAdjustFood(stack);
        if (data != null) {
            float env = PlayerTemperatureData.getCapability(player).map(PlayerTemperatureData::getEnvTemp).orElse(0f);
            float heat = (float) (data.getHeat(stack, env) * FHConfig.SERVER.tempSpeed.get());
            if (heat != 0) {
                String s = TemperatureDisplayHelper.toTemperatureDeltaFloatString(heat);
                FHLang.translate("tooltip.food_temp_change")
                        .style(ChatFormatting.GRAY)
                        .addTo(list);

                // create a progress that maps heat from -1 to 1 to 0 to 10 based on absolute value
                int progress = (int) Mth.clamp(Math.abs(heat) * 10, 1, 10);

                LangBuilder builder = FHLang.builder()
                        .add(FHLang.text(s + " " + TooltipHelper.makeProgressBar(10, progress))
                                .style(heat < 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD));
                builder.addTo(list);
            }
        }
        return list;
    }
}

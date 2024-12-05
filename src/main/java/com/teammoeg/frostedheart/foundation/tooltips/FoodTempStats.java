package com.teammoeg.frostedheart.foundation.tooltips;

import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.teammoeg.frostedheart.content.climate.player.ITempAdjustFood;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import com.teammoeg.frostedheart.infrastructure.data.FHDataManager;
import com.teammoeg.frostedheart.util.TemperatureDisplayHelper;
import com.teammoeg.frostedheart.util.lang.Components;
import com.teammoeg.frostedheart.util.lang.Lang;
import com.teammoeg.frostedheart.util.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;

public class FoodTempStats implements TooltipModifier {
    protected final Item item;
    public FoodTempStats(Item item) {
        this.item = item;
    }

    // Important: We cannot do the getTemp check because data is not available at this time.
//    @Nullable
//    public static FoodTempStats create(Item item) {
//        FHMain.LOGGER.debug("Creating FoodTempStats for " + item);
//        if (FHDataManager.getTempAdjustFood(item) != null) {
//            FHMain.LOGGER.debug("Created FoodTempStats for " + item);
//            return new FoodTempStats(item);
//        }
//        FHMain.LOGGER.debug("Failed to create FoodTempStats for " + item);
//        return null;
//    }

    @Override
    public void modify(ItemTooltipEvent context) {
        List<Component> stats = getFoodStats(item, context.getItemStack(), context.getEntity());
        if (!stats.isEmpty()) {
            List<Component> tooltip = context.getToolTip();
            tooltip.add(Components.immutableEmpty());
            tooltip.addAll(stats);
        }
    }

    public static List<Component> getFoodStats(Item item, ItemStack stack, Player player) {
        List<Component> list = new ArrayList<>();
        ITempAdjustFood data = FHDataManager.getTempAdjustFood(stack);
        if (data != null) {
            float env = PlayerTemperatureData.getCapability(player).map(PlayerTemperatureData::getEnvTemp).orElse(0f);
            float heat = (float) (data.getHeat(stack, env) * FHConfig.SERVER.tempSpeed.get());
            String s = TemperatureDisplayHelper.toTemperatureDeltaFloatString(heat);
            Lang.translate("tooltip", "temp.food")
                    .style(ChatFormatting.GRAY)
                    .addTo(list);

            // create a progress that maps heat from -1 to 1 to 0 to 10 based on absolute value
            int progress = Mth.ceil(Mth.clamp(Math.abs(heat) * 3, 0, 3));

            LangBuilder builder = Lang.builder()
                    .add(Lang.text(s + " " + TooltipHelper.makeProgressBar(3, progress))
                            .style(heat < 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD));
            builder.addTo(list);
        }
        return list;
    }
}

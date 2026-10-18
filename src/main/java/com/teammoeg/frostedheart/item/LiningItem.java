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

package com.teammoeg.frostedheart.item;

import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.climate.data.ArmorTempData;
import com.teammoeg.frostedheart.content.climate.player.PlayerTemperatureData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LiningItem extends FHBaseItem {
    public LiningItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        var data = ArmorTempData.getData(pStack, PlayerTemperatureData.BodyPart.HANDS);
        if (data != null) {
            pTooltipComponents.add(Component.empty());
            pTooltipComponents.add(Component.translatable("item.frostedheart.modifiers.hands").withStyle(ChatFormatting.GRAY));
            float insulation = data.getInsulation();
            float heat = data.getHeatProof();
            float fluid = data.getFluidResistance();
            if (insulation != 0) {
                var text = Component.literal((insulation>0?"+":"") + ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(insulation) + " ")
                        .append(Component.translatable("attribute." + FHMain.MODID + ".insulation"))
                        .withStyle(insulation > 0 ? ChatFormatting.BLUE : ChatFormatting.RED);
                pTooltipComponents.add(text);
            }
            if (fluid != 0) {
                var text = Component.literal((fluid>0?"+":"") + (int)(fluid*100) + "% ")
                        .append(Component.translatable("attribute." + FHMain.MODID + ".windproof"))
                        .withStyle(fluid > 0 ? ChatFormatting.BLUE : ChatFormatting.RED);
                pTooltipComponents.add(text);
            }
            if (heat != 0) {
                var text = Component.literal((heat>0?"+":"") + (int)(heat*100) + "% ")
                        .append(Component.translatable("attribute." + FHMain.MODID + ".heatproof"))
                        .withStyle(heat > 0 ? ChatFormatting.BLUE : ChatFormatting.RED);
                pTooltipComponents.add(text);
            }
        }
    }
}

package com.teammoeg.frostedheart.base.tooltip;

import com.simibubi.create.foundation.utility.Components;
import com.teammoeg.frostedheart.bootstrap.client.FHKeyMappings;
import com.teammoeg.frostedheart.util.lang.Lang;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.minecraft.ChatFormatting.*;
import static net.minecraft.ChatFormatting.DARK_GRAY;

public class KeyControlledDesc {
    List<Component> lines;
    List<Component> linesOnS;
    List<Component> linesOnCtrl;

    // initialize with empty lines
    public KeyControlledDesc(List<Component> shiftLines, List<Component> ctrlLines) {
        this.lines = new ArrayList<>();
        this.linesOnS = shiftLines;
        this.linesOnCtrl = ctrlLines;

        boolean hasDescription = !this.linesOnS.isEmpty();
        boolean hasControls = !this.linesOnCtrl.isEmpty();

        if (hasDescription || hasControls) {
            String[] holdDesc = Lang.translateTooltip("holdForTemperature", "$")
                    .getString()
                    .split("\\$");
            String[] holdCtrl = Lang.translateTooltip("holdForControls", "$")
                    .getString()
                    .split("\\$");
            MutableComponent keyShift = Lang.translateTooltip("keyS");
            MutableComponent keyCtrl = Lang.translateTooltip("keyCtrl");

            for (List<Component> list : Arrays.asList(lines, this.linesOnS, this.linesOnCtrl)) {
                boolean shift = list == this.linesOnS;
                boolean ctrl = list == this.linesOnCtrl;
                if (holdDesc.length != 2 || holdCtrl.length != 2) {
                    list.add(0, Components.literal("Invalid lang formatting!"));
                    continue;
                }

                if (hasControls) {
                    MutableComponent tabBuilder = Components.empty();
                    tabBuilder.append(Components.literal(holdCtrl[0]).withStyle(DARK_GRAY));
                    tabBuilder.append(keyCtrl.plainCopy()
                            .withStyle(ctrl ? WHITE : GRAY));
                    tabBuilder.append(Components.literal(holdCtrl[1]).withStyle(DARK_GRAY));
                    list.add(0, tabBuilder);
                }

                if (hasDescription) {
                    MutableComponent tabBuilder = Components.empty();
                    tabBuilder.append(Components.literal(holdDesc[0]).withStyle(DARK_GRAY));
                    tabBuilder.append(keyShift.plainCopy()
                            .withStyle(shift ? WHITE : GRAY));
                    tabBuilder.append(Components.literal(holdDesc[1]).withStyle(DARK_GRAY));
                    list.add(0, tabBuilder);
                }

                if (shift || ctrl)
                    list.add(hasDescription && hasControls ? 2 : 1, Components.immutableEmpty());

            }
        }

        if (!hasDescription) {
            this.linesOnCtrl.clear();
            this.linesOnS.addAll(lines);
        }
        if (!hasControls) {
            this.linesOnCtrl.clear();
            this.linesOnCtrl.addAll(lines);
        }
    }

    public List<Component> getCurrentLines() {
        if (FHKeyMappings.hasSDown()) {
            return linesOnS;
        } else if (Screen.hasControlDown()) {
            return linesOnCtrl;
        } else {
            return lines;
        }
    }

}

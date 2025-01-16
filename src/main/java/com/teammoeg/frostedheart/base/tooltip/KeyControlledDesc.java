package com.teammoeg.frostedheart.base.tooltip;

import com.teammoeg.frostedheart.bootstrap.client.FHKeyMappings;
import com.teammoeg.frostedheart.util.lang.Components;
import com.teammoeg.frostedheart.util.lang.Lang;
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
    int key1;
    int key2;
    String key1Desc;
    String key2Desc;
    String key1Translation;
    String key2Translation;

    // initialize with empty lines
    public KeyControlledDesc(List<Component> shiftLines, List<Component> ctrlLines, int key1, int key2,
                             String key1Desc, String key2Desc,
                             String key1Translation, String key2Translation) {
        this.lines = new ArrayList<>();
        this.linesOnS = shiftLines;
        this.linesOnCtrl = ctrlLines;
        this.key1 = key1;
        this.key2 = key2;
        this.key1Desc = key1Desc;
        this.key2Desc = key2Desc;
        this.key1Translation = key1Translation;
        this.key2Translation = key2Translation;

        boolean hasDescription = !this.linesOnS.isEmpty();
        boolean hasControls = !this.linesOnCtrl.isEmpty();

        if (hasDescription || hasControls) {
            String[] holdDesc = Lang.translateTooltip(this.key1Translation, "$")
                    .getString()
                    .split("\\$");
            String[] holdCtrl = Lang.translateTooltip(this.key2Translation, "$")
                    .getString()
                    .split("\\$");
            MutableComponent keyShiftTooltip = Lang.text(this.key1Desc).component();
            MutableComponent keyCtrlTooltip = Lang.text(this.key2Desc).component();

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
                    tabBuilder.append(keyCtrlTooltip.plainCopy()
                            .withStyle(ctrl ? WHITE : GRAY));
                    tabBuilder.append(Components.literal(holdCtrl[1]).withStyle(DARK_GRAY));
                    list.add(0, tabBuilder);
                }

                if (hasDescription) {
                    MutableComponent tabBuilder = Components.empty();
                    tabBuilder.append(Components.literal(holdDesc[0]).withStyle(DARK_GRAY));
                    tabBuilder.append(keyShiftTooltip.plainCopy()
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
        if (FHKeyMappings.isDown(key1)) {
            return linesOnS;
        } else if (FHKeyMappings.isDown(key2)) {
            return linesOnCtrl;
        } else {
            return lines;
        }
    }

}

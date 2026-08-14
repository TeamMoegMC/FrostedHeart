/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.ui.tips.client;

import com.teammoeg.chorda.math.Colors;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.observation.TownSignalNotice;
import com.teammoeg.frostedheart.content.town.observation.TownSignalTipPresentationModel;
import com.teammoeg.frostedheart.content.ui.tips.Tip;
import com.teammoeg.frostedheart.content.ui.tips.TipManager;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/** Converts a safe town-notice packet into one transient right-side Tip. */
@OnlyIn(Dist.CLIENT)
public final class TownSignalTipPresentation {
    private TownSignalTipPresentation() {
    }

    public static void display(long notificationId, List<TownSignalNotice> notices) {
        if (!FHConfig.CLIENT.enableTip.get() || !FHConfig.CLIENT.enableTownEventTips.get()
                || notices.isEmpty()) {
            return;
        }
        TownSignalTipPresentationModel.Presentation presentation =
                TownSignalTipPresentationModel.create(notices);
        List<Component> contents = new ArrayList<>();
        contents.add(Component.translatable(presentation.titleKey()));
        for (TownSignalNotice notice : presentation.visibleEvents()) {
            Component description = TownSignalTipPresentationModel.usesAffectedCount(notice.type())
                    ? Component.translatable(TownSignalTipPresentationModel.eventKey(notice.type()),
                            notice.affectedCount())
                    : Component.translatable(TownSignalTipPresentationModel.eventKey(notice.type()));
            contents.add(Component.literal("• ").append(description));
        }
        if (presentation.overflowCount() > 0) {
            contents.add(Component.translatable("tips.frostedheart.town.overflow",
                    presentation.overflowCount()));
        }

        Tip tip = Tip.builder("/town/events/" + Long.toUnsignedString(notificationId))
                .components(contents)
                .temporary()
                .onceOnly(false)
                .displayTime(presentation.displayTimeMillis())
                .fontColor(presentation.fontColor())
                .backgroundColor(Colors.BLACK)
                .pin(presentation.preemptsTutorial())
                .clickAction(FHMain.rl("open_town_events"), "")
                .build();
        TipManager.display().general(tip);
    }
}

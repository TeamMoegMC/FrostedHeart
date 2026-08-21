/*
 * Copyright (c) 2026 TeamMoeg
 *
 * This file is part of Frosted Heart.
 */

package com.teammoeg.frostedheart.content.ui.tips.client;

import com.teammoeg.chorda.math.Colors;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.transport.TownTransportShortageNotice;
import com.teammoeg.frostedheart.content.town.transport.TownTransportShortageTipPresentationModel;
import com.teammoeg.frostedheart.content.ui.tips.Tip;
import com.teammoeg.frostedheart.content.ui.tips.TipManager;
import com.teammoeg.frostedheart.infrastructure.config.FHConfig;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/** Converts validated transport-shortage numbers into one localized transient Tip. */
@OnlyIn(Dist.CLIENT)
public final class TownTransportShortageTipPresentation {
    private TownTransportShortageTipPresentation() {
    }

    public static void display(long notificationId, List<TownTransportShortageNotice> notices) {
        if (!FHConfig.CLIENT.enableTip.get() || !FHConfig.CLIENT.enableTownEventTips.get()
                || notices.isEmpty()) {
            return;
        }
        TownTransportShortageTipPresentationModel.Presentation presentation =
                TownTransportShortageTipPresentationModel.create(notices);
        List<Component> contents = new ArrayList<>();
        contents.add(Component.translatable(presentation.titleKey()));
        for (TownTransportShortageNotice notice : presentation.visibleNotices()) {
            contents.add(Component.translatable(
                    presentation.detailKey(),
                    TownTransportShortageTipPresentationModel.formatCapacity(notice.totalCapacity()),
                    TownTransportShortageTipPresentationModel.formatCapacity(notice.reservedCapacity()),
                    TownTransportShortageTipPresentationModel.formatCapacity(notice.shortfall()),
                    TownTransportShortageTipPresentationModel.formatScale(notice.effectiveRateScale())));
        }
        if (presentation.overflowCount() > 0) {
            contents.add(Component.translatable(
                    presentation.overflowKey(), presentation.overflowCount()));
        }

        Tip tip = Tip.builder("/town/transport-shortage/"
                        + Long.toUnsignedString(notificationId))
                .components(contents)
                .temporary()
                .onceOnly(false)
                .displayTime(8_000)
                .fontColor(0xFFFF5555)
                .backgroundColor(Colors.BLACK)
                .pin(true)
                .clickAction(FHMain.rl("open_town_transport"), "")
                .build();
        TipManager.display().general(tip);
    }
}

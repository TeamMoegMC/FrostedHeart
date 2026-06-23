package com.teammoeg.frostedheart.content.town.resident;

import com.teammoeg.chorda.client.cui.widgets.TextButton;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.network.WanderingRefugeeOpenTradeGUIMessage;
import com.teammoeg.frostedheart.content.town.network.WanderingRefugeeRecruitMessage;
import com.teammoeg.frostedheart.content.ui.dialogue.DialogueOverlay;
import com.teammoeg.frostedheart.content.ui.dialogue.DialogueScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WanderingRefugeeClientHelper {
    public static void openScreen(WanderingRefugee entity) {
        var trade = TextButton.create(DialogueOverlay.INSTANCE, Component.translatable("gui.frostedheart.wandering_refugee.trade_button"), FlatIcon.TRADE.toCIcon(), mb ->
            FHNetwork.INSTANCE.sendToServer(new WanderingRefugeeOpenTradeGUIMessage(entity.getId()))
        );
        var recruit = TextButton.create(DialogueOverlay.INSTANCE, Component.translatable("gui.frostedheart.wandering_refugee.recruit_button"), FlatIcon.GAIN.toCIcon(), mb ->
            FHNetwork.INSTANCE.sendToServer(new WanderingRefugeeRecruitMessage(entity.getId()))
        );
//        recruit.setEnabled(false); TODO 无法容纳更多居民时禁用按钮
        DialogueScreen.open(true, trade, recruit);
//        Minecraft.getInstance().setScreen(new WanderingRefugeeScreen(entity));
    }
}

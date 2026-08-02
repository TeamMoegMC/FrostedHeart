package com.teammoeg.frostedheart.content.town.resident;

import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.widgets.TextButton;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.chorda.dataholders.team.CClientTeamDataManager;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.bootstrap.common.FHSpecialDataTypes;
import com.teammoeg.frostedheart.content.town.TeamTownData;
import com.teammoeg.frostedheart.content.town.event.ITownDataUpdateListener;
import com.teammoeg.frostedheart.content.town.network.WanderingRefugeeOpenTradeGUIMessage;
import com.teammoeg.frostedheart.content.town.network.WanderingRefugeeRecruitMessage;
import com.teammoeg.frostedheart.content.ui.dialogue.DialogueOverlay;
import com.teammoeg.frostedheart.content.ui.dialogue.DialogueScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WanderingRefugeeClientHelper {
    /** 当前对话注册的城镇数据监听器，对话关闭时自我移除，避免跨对话累积。 */
    private static ITownDataUpdateListener refreshListener = null;

    public static void openScreen(WanderingRefugee entity) {
        var trade = TextButton.create(DialogueOverlay.INSTANCE, Component.translatable("gui.frostedheart.wandering_refugee.trade_button"), FlatIcon.TRADE.toCIcon(), mb ->
            FHNetwork.INSTANCE.sendToServer(new WanderingRefugeeOpenTradeGUIMessage(entity.getId()))
        );
        // 招募按钮：禁用时显示"无空余房屋"提示，状态翻转时提示自动跟随
        var recruit = new TextButton(DialogueOverlay.INSTANCE, Component.translatable("gui.frostedheart.wandering_refugee.recruit_button"), FlatIcon.GAIN.toCIcon()) {
            @Override
            public void onClicked(MouseButton button) {
                FHNetwork.INSTANCE.sendToServer(new WanderingRefugeeRecruitMessage(entity.getId()));
            }

            @Override
            public void getTooltip(TooltipBuilder list) {
                if (!isEnabled()) {
                    list.accept(Component.translatable("gui.frostedheart.wandering_refugee.recruit_disabled_no_housing"));
                }
            }
        };

        boolean canRecruit = canAddResident();
        recruit.setEnabled(canRecruit);

        // 注册城镇数据增量监听：建筑/居民变化（新建房屋、居民死亡释放槽位等）时刷新招募可用状态。
        // 复用与城镇 GUI 相同的 addClientListener/removeClientListener 机制；监听在对话关闭
        // （DialogueOverlay 被禁用）时自我移除。
        if (refreshListener != null) {
            TeamTownData.removeClientListener(refreshListener);
        }
        final TextButton recruitButton = recruit;
        refreshListener = new ITownDataUpdateListener() {
            @Override
            public void onBuildingsChanged() {
                refreshRecruit();
            }

            @Override
            public void onResidentsChanged() {
                refreshRecruit();
            }

            private void refreshRecruit() {
                if (!DialogueOverlay.INSTANCE.isEnabled()) {
                    TeamTownData.removeClientListener(this);
                    refreshListener = null;
                    return;
                }
                recruitButton.setEnabled(canAddResident());
            }
        };
        TeamTownData.addClientListener(refreshListener);

        DialogueScreen.open(true, trade, recruit);
//        Minecraft.getInstance().setScreen(new WanderingRefugeeScreen(entity));
    }

    /**
     * 从客户端城镇数据快照判断能否再招募一名居民。
     * <p>
     * Whether another resident can be recruited, based on the client-side town data snapshot.
     *
     * @return 可招募则返回 true / true if another resident can be recruited
     */
    private static boolean canAddResident() {
        TeamTownData townData = CClientTeamDataManager.INSTANCE.getInstance().getData(FHSpecialDataTypes.TOWN_DATA);
        return townData != null && townData.createTeamTown().canAddResident();
    }
}

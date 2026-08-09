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
        // 交易按钮：幼儿禁用（幼儿不会交易，但可招募），悬停显示提示
        var trade = new TextButton(DialogueOverlay.INSTANCE,
                Component.translatable("gui.frostedheart.wandering_refugee.trade_button"),
                FlatIcon.TRADE.toCIcon()) {
            @Override
            public void onClicked(MouseButton button) {
                FHNetwork.INSTANCE.sendToServer(new WanderingRefugeeOpenTradeGUIMessage(entity.getId()));
            }

            @Override
            public void getTooltip(TooltipBuilder list) {
                if (!isEnabled()) {
                    list.accept(Component.translatable("gui.frostedheart.wandering_refugee.trade_disabled_too_young"));
                }
            }
        };
        trade.setEnabled(entity.getAgeGroup() != Resident.AGE_INFANT);
        // 招募按钮：标题带年龄组；禁用时显示"无空余房屋"提示，状态翻转时提示自动跟随
        var recruit = new TextButton(DialogueOverlay.INSTANCE,
                Component.translatable("gui.frostedheart.wandering_refugee.recruit_button")
                        .append(Component.literal(" ("))
                        .append(Component.translatable(Resident.ageLangKey(entity.getAgeGroup())))
                        .append(Component.literal(")")),
                FlatIcon.GAIN.toCIcon()) {
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
            /** 最近一次已求值的全量同步批次：同一批内建筑/居民双回调之间数据不变，仅求值一次 */
            private long lastRefreshedSyncBatch = -1L;

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
                // 全量包批内去重：同批重复回调（居民包+建筑包顺序处理）跳过，canAddResident 只求值一次；
                // 批外（增量包路径）每次照常求值，早晨结算的双求值保持原样
                long syncBatch = TeamTownData.getClientSyncBatchId();
                if (TeamTownData.isInClientBatchFire() && syncBatch == lastRefreshedSyncBatch) {
                    return;
                }
                lastRefreshedSyncBatch = syncBatch;
                recruitButton.setEnabled(canAddResident());
            }
        };
        TeamTownData.addClientListener(refreshListener);

        // 对话关闭时（任何方式）立即移除监听器，避免驻留到下次打开；保留上述自移除逻辑作双保险
        DialogueOverlay.closeCallback = () -> {
            TeamTownData.removeClientListener(refreshListener);
            refreshListener = null;
        };

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
        // 用 getOptional 而非 getData：数据缺失时只读判空，不向客户端 holder 塞入空城镇数据
        return CClientTeamDataManager.INSTANCE.getInstance()
                .getOptional(FHSpecialDataTypes.TOWN_DATA)
                .map(townData -> townData.createTeamTown().canAddResident())
                .orElse(false);
    }
}

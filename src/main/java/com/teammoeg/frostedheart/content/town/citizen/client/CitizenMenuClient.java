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

package com.teammoeg.frostedheart.content.town.citizen.client;

import com.teammoeg.chorda.client.cui.base.MouseButton;
import com.teammoeg.chorda.client.cui.base.TooltipBuilder;
import com.teammoeg.chorda.client.cui.widgets.TextButton;
import com.teammoeg.chorda.client.icon.FlatIcon;
import com.teammoeg.frostedheart.FHNetwork;
import com.teammoeg.frostedheart.content.town.citizen.CitizenNames;
import com.teammoeg.frostedheart.content.town.citizen.sync.C2SCitizenActionPacket;
import com.teammoeg.frostedheart.content.town.resident.WanderingRefugeeClientHelper;
import com.teammoeg.frostedheart.content.ui.dialogue.DialogueOverlay;
import com.teammoeg.frostedheart.content.ui.dialogue.DialogueScreen;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 居民交互菜单（客户端）：模仿流浪难民的对话式 GUI——
 * {@link DialogueScreen} 叠层 + 图标按钮列，按钮一律发 C2S 动作包，
 * 服务端权威校验后执行（一切交互都是 RPC）。
 * 菜单数据来自 {@link ClientCitizenCache} 的本地快照，姓名优先用 spawn 包
 * 同步的真实姓名（城镇托管居民），未托管居民按稳定 id 确定性生成，
 * 打开菜单本身不产生任何网络流量。
 * <p>
 * Citizen interaction menu (client): mirrors the wandering refugee's
 * dialogue-style GUI — a {@link DialogueScreen} overlay with icon buttons.
 * Every button sends a C2S action packet which the server validates
 * authoritatively (all interactions are RPCs). Menu data comes from the
 * local {@link ClientCitizenCache} snapshot; names come from the spawn-synced
 * real name when available (town-backed) and are deterministically derived
 * from the stable id otherwise, so opening the menu costs no traffic.
 */
@OnlyIn(Dist.CLIENT)
public final class CitizenMenuClient {

	private CitizenMenuClient() {
	}

	/**
	 * 打开某居民的交互菜单。
	 * <p>
	 * Opens the interaction menu for a citizen.
	 *
	 * @param citizenId 居民稳定 id / citizen stable id
	 */
	public static void open(int citizenId) {
		ClientCitizen c = ClientCitizenCache.get(citizenId);
		if (c == null)
			return; // 缓存已消失（AOI 移出竞态），忽略

		// 姓名标签：城镇托管居民显示 spawn 包携带的真实姓名，未托管回退 id 派生名。
		// 禁用按钮当静态文本用——点击无动作、不发包（onClicked 空实现满足抽象方法）。
		var nameLabel = new TextButton(DialogueOverlay.INSTANCE,
				Component.literal(c.name.isEmpty() ? CitizenNames.fullName(citizenId) : c.name),
				FlatIcon.INFO.toCIcon()) {
			@Override
			public void onClicked(MouseButton button) {
				// 姓名标签：无动作
			}
		};
		nameLabel.setEnabled(false);

		// 交易按钮：FH Trade 系统入口（C2S → TradeHandler.openTradeScreen）
		var trade = new TextButton(DialogueOverlay.INSTANCE,
				Component.translatable("gui.frostedheart.citizen.trade_button"),
				FlatIcon.TRADE.toCIcon()) {
			@Override
			public void onClicked(MouseButton button) {
				FHNetwork.INSTANCE.sendToServer(new C2SCitizenActionPacket(citizenId, C2SCitizenActionPacket.TRADE));
			}
		};

		// 闲聊按钮：回一句按日轮换的台词
		var chat = new TextButton(DialogueOverlay.INSTANCE,
				Component.translatable("gui.frostedheart.citizen.chat_button"),
				FlatIcon.INFO.toCIcon()) {
			@Override
			public void onClicked(MouseButton button) {
				FHNetwork.INSTANCE.sendToServer(new C2SCitizenActionPacket(citizenId, C2SCitizenActionPacket.CHAT));
			}
		};

		var knowledge = new TextButton(DialogueOverlay.INSTANCE,
				Component.translatable("gui.frostedheart.person_knowledge.talk_button"),
				FlatIcon.PIN.toCIcon()) {
			@Override
			public void onClicked(MouseButton button) {
				FHNetwork.INSTANCE.sendToServer(new C2SCitizenActionPacket(
						citizenId, C2SCitizenActionPacket.ASK_EXPERIENCE));
			}
		};

		// 招募按钮：禁用时显示"无空余房屋"提示（与难民招募同款交互）
		var recruit = new TextButton(DialogueOverlay.INSTANCE,
				Component.translatable("gui.frostedheart.citizen.recruit_button"),
				FlatIcon.GAIN.toCIcon()) {
			@Override
			public void onClicked(MouseButton button) {
				FHNetwork.INSTANCE.sendToServer(new C2SCitizenActionPacket(citizenId, C2SCitizenActionPacket.RECRUIT));
			}

			@Override
			public void getTooltip(TooltipBuilder list) {
				if (!isEnabled()) {
					list.accept(Component.translatable("gui.frostedheart.citizen.recruit_disabled_no_housing"));
				}
			}
		};
		recruit.setEnabled(WanderingRefugeeClientHelper.canAddResident());

		DialogueScreen.open(true, nameLabel, trade, chat, knowledge, recruit);
	}
}

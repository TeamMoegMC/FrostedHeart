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

package com.teammoeg.frostedheart.content.town.citizen.sync;

import java.util.function.Supplier;
import java.util.UUID;

import com.teammoeg.chorda.dataholders.team.CTeamDataManager;
import com.teammoeg.chorda.dataholders.team.TeamDataHolder;
import com.teammoeg.chorda.network.CMessage;
import com.teammoeg.frostedheart.content.climate.gamedata.climate.WorldClimate;
import com.teammoeg.frostedheart.content.trade.TradeHandler;
import com.teammoeg.frostedheart.content.town.TeamTown;
import com.teammoeg.frostedheart.content.town.citizen.CitizenNames;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenContainer;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenPresence;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenSim;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenSimScheduler;
import com.teammoeg.frostedheart.content.town.resident.PersonKnowledgeDialogue;
import com.teammoeg.frostedheart.content.town.resident.Resident;
import com.teammoeg.frostedresearch.knowledge.PersonKnowledgeOverlay;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * 客户端 → 服务端：对某居民执行菜单动作（闲聊 / 交易 / 招募）。
 * 服务端权威校验：id 存活、同维度、距离 ≤ 8 格——一切交互都是 RPC，
 * 菜单数据源是 CitizenSim，全部读写落在服务端权威数据上。
 * 招募链路模仿 {@code WanderingRefugeeRecruitMessage}：队伍与空房位校验 →
 * 加入 TeamTown → 从模拟移除。交易打开原版交易界面（{@link CitizenMerchant}）。
 * <p>
 * Client → server: performs a menu action (chat / trade / recruit) on a
 * citizen. Server-authoritative validation: id alive, same dimension,
 * distance ≤ 8 — every interaction is an RPC and the menu data source is
 * CitizenSim, so all reads and writes hit server-authoritative data.
 * The recruit path mirrors {@code WanderingRefugeeRecruitMessage}: team and
 * housing checks → add to TeamTown → remove from the simulation. Trading
 * opens the FH trade screen ({@code TradeHandler#openTradeScreen}) with the
 * FH trade data (null parent, empty policy snapshot — interface-only, no
 * concrete trade content).
 */
public final class C2SCitizenActionPacket implements CMessage {

	/** 闲聊 / Chat */
	public static final byte CHAT = 0;
	/** 交易 / Trade */
	public static final byte TRADE = 1;
	/** 招募 / Recruit */
	public static final byte RECRUIT = 2;
	/** 询问持久化背景经验 / Ask about persistent background experience. */
	public static final byte ASK_EXPERIENCE = 3;

	/** 最大交互距离（方块） / Max interaction distance in blocks */
	private static final double MAX_DIST = 8.0;

	private final int citizenId;
	private final byte action;

	public C2SCitizenActionPacket(int citizenId, byte action) {
		this.citizenId = citizenId;
		this.action = action;
	}

	public C2SCitizenActionPacket(FriendlyByteBuf buf) {
		this.citizenId = buf.readVarInt();
		this.action = buf.readByte();
	}

	@Override
	public void encode(FriendlyByteBuf buf) {
		buf.writeVarInt(citizenId);
		buf.writeByte(action);
	}

	@Override
	public void handle(Supplier<NetworkEvent.Context> context) {
		NetworkEvent.Context ctx = context.get();
		ServerPlayer sender = ctx.getSender();
		if (sender == null) {
			ctx.setPacketHandled(true);
			return;
		}
		ctx.enqueueWork(() -> {
			ServerLevel level = sender.serverLevel();
			CitizenSimScheduler sched = CitizenSimScheduler.get(level);
			CitizenContainer c = sched.findById(citizenId);
			if (c == null)
				return;
			CitizenSim sim = c.sim();
			int i = sim.indexOf(citizenId);
			if (i < 0)
				return;
			if (!sched.sync.isTracked(sender, citizenId))
				return;
			if (!CitizenPresence.interactionAllowed(sim.state[i]))
				return;
			double dx = sim.px[i] / 1024.0 - sender.getX();
			double dy = sim.py[i] / 1024.0 - sender.getY();
			double dz = sim.pz[i] / 1024.0 - sender.getZ();
			if (dx * dx + dy * dy + dz * dz > MAX_DIST * MAX_DIST)
				return;
			switch (action) {
			case CHAT -> handleChat(sender, level, c);
			// 交易：FH Trade 系统入口（居民无实体，data 用 null parent 构造；policytype=null → 空政策快照，仅接通接口）
			case TRADE -> TradeHandler.openTradeScreen(sender, c.getTradeData(citizenId));
			case RECRUIT -> handleRecruit(sender, level, sched, c, i);
			case ASK_EXPERIENCE -> handleExperience(sender, sim, i);
			default -> {
			}
			}
		});
		ctx.setPacketHandled(true);
	}

	private void handleExperience(ServerPlayer sender, CitizenSim sim, int index) {
		if (CTeamDataManager.get(sender) == null) {
			sender.displayClientMessage(Component.translatable("message.frostedheart.citizen.need_team"), false);
			return;
		}
		UUID residentId = new UUID(sim.uuidHi[index], sim.uuidLo[index]);
		Resident resident = TeamTown.from(sender).getResident(residentId).orElse(null);
		PersonKnowledgeOverlay overlay = resident != null && resident.getSimId() == citizenId
				? resident.getKnowledgeOverlay() : PersonKnowledgeOverlay.UNINITIALIZED;
		PersonKnowledgeDialogue.shareFirst(sender, overlay, "person:" + residentId);
	}

	/**
	 * 闲聊：回一句按 id+世界日确定性轮换的台词，附当前行为状态。
	 * <p>
	 * Chat: replies with a deterministic daily-rotated line plus the current
	 * behavior state.
	 */
	private void handleChat(ServerPlayer sender, ServerLevel level, CitizenContainer c) {
		long day = WorldClimate.getWorldDay(level);
		int line = CitizenNames.chatLine(citizenId, day);
		String name = c.getCitizenName(citizenId);
		if (name == null)
			name = CitizenNames.fullName(citizenId); // 未托管居民回退 id 派生名 / unmanaged: fall back to the id-derived name
		sender.displayClientMessage(Component.literal("<" + name + "> ")
				.append(Component.translatable("message.frostedheart.citizen.chat." + line)), false);
	}

	/**
	 * 招募：模仿流浪难民招募——队伍校验 → 空房位校验 → 加入 TeamTown → 移出模拟。
	 * 数据居民一律按成年招募。加入后事件驱动立即出生镇条目（新 uuid 的 town 居民），
	 * 旧未托管条目经调度器统一移除（despawn 广播）——比原 1Hz 延迟更小。
	 * <p>
	 * Recruit: mirrors the wandering refugee path — team check → vacancy check →
	 * add to TeamTown → remove from the simulation. Simulated citizens are
	 * always recruited as adults. The add drives an immediate event-spawn of the
	 * town entry (a town resident with a new uuid); the old unmanaged entry is
	 * removed via the scheduler's unified removal (despawn broadcast) — faster
	 * than the old 1 Hz latency.
	 */
	private void handleRecruit(ServerPlayer sender, ServerLevel level, CitizenSimScheduler sched, CitizenContainer c,
			int i) {
		CitizenSim sim = c.sim();
		// 城镇托管居民已是居民（nameCache 有条目）：招募无意义，直接提示
		if (c.getCitizenName(citizenId) != null) {
			sender.displayClientMessage(Component.translatable("message.frostedheart.citizen.already_resident"), false);
			return;
		}
		// 无队伍玩家不能招募（CTeamDataManager.get 无队伍返回 null）
		TeamDataHolder holder = CTeamDataManager.get(sender);
		if (holder == null) {
			sender.displayClientMessage(Component.translatable("message.frostedheart.citizen.need_team"), false);
			return;
		}
		TeamTown town = TeamTown.from(sender);
		String name = CitizenNames.fullName(citizenId);
		Resident resident = new Resident(CitizenNames.firstName(citizenId), CitizenNames.lastName(citizenId));
		if (town.addResident(resident)) {
			double bx = sim.px[i] / 1024.0;
			double by = sim.py[i] / 1024.0;
			double bz = sim.pz[i] / 1024.0;
			// 与难民招募一致的庆祝粒子
			for (int k = 0; k < 16; k++) {
				level.addParticle(ParticleTypes.EXPLOSION, bx, by, bz, Math.random(), Math.random(), 0.01D);
				level.addParticle(ParticleTypes.END_ROD, bx, by, bz, Math.random(), Math.random(), 0.01D);
			}
			sender.displayClientMessage(Component.translatable("message.frostedheart.citizen.recruited", name), false);
			sched.remove(level, citizenId);
		} else {
			sender.displayClientMessage(Component.translatable("message.frostedheart.citizen.cannot_accommodate"), false);
		}
	}
}

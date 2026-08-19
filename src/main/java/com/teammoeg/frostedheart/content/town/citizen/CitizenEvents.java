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

package com.teammoeg.frostedheart.content.town.citizen;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.teammoeg.frostedheart.FHMain;
import com.teammoeg.frostedheart.content.town.ai_town.AITownData;
import com.teammoeg.frostedheart.content.town.ai_town.AITownManager;
import com.teammoeg.frostedheart.content.town.citizen.nav.NavJobExecutor;
import com.teammoeg.frostedheart.content.town.citizen.sim.CitizenSimScheduler;
import com.teammoeg.frostedheart.content.town.citizen.sync.SyncEngine;
import com.teammoeg.frostedheart.content.town.resident.Resident;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 居民系统服务端事件：维度 tick 驱动与调试命令。
 * 调试命令（需要 OP 权限 2）：
 * <ul>
 *   <li>{@code /fhcitizen spawn <count> [radius]} — 在以执行者为中心的半径内生成居民；</li>
 *   <li>{@code /fhcitizen clear} — 移除本维度全部居民；</li>
 *   <li>{@code /fhcitizen count} — 查询本维度居民数量。</li>
 * </ul>
 * <p>
 * Server-side events for the citizen system: level tick driver and debug
 * commands (OP level 2). See the Chinese list above for usage.
 */
@Mod.EventBusSubscriber(modid = FHMain.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CitizenEvents {

	private CitizenEvents() {
	}

	/**
	 * 每 tick 驱动各服务端维度的居民模拟。
	 * <p>
	 * Drives the citizen simulation of each server level every tick.
	 *
	 * @param event tick 事件 / the tick event
	 */
	@SubscribeEvent
	public static void onLevelTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		if (!(event.level instanceof ServerLevel level))
			return;
		CitizenSimScheduler sched = CitizenSimScheduler.get(level);
		// 恒调用：全部容器为空时 sync.flush 仍会广播 pendingRemoval
		// （修复最后一名居民 despawn 包滞留的已知问题）；注册表聚合在 tick 内部 gt%20 驱动。
		// Always tick: with all containers empty, sync.flush still broadcasts
		// pendingRemoval (fixes the stale last-despawn bug); the registry
		// aggregation runs inside at gt%20.
		sched.tick(level);
	}

	/** Applies the cross-dimension citizen presentation budget after level ticks. */
	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.END)
			SyncEngine.refreshServerVisibility(event.getServer());
	}

	/**
	 * 服务器停止时关闭寻路线程池并清空调度器注册表（不持久化，运行期状态）。
	 * <p>
	 * Shuts down the pathfinding thread pool and clears the scheduler registry
	 * (runtime state only, nothing persisted) when the server stops.
	 *
	 * @param event 服务器停止事件 / the server stopping event
	 */
	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		CitizenSimScheduler.markAllDirty();
		NavJobExecutor.shutdown();
		CitizenSimScheduler.resetAll();
	}

	/**
	 * 服务器完全停止后清空全局模拟存储引用（AITownManager.reset）——
	 * 必须在存档保存完成之后（ServerStopping 在保存之前触发，清空静态表会导致
	 * 停服保存写出空文件、AI 镇与玩家镇模拟重启即丢）。
	 * <p>
	 * Clears the global simulation store references (AITownManager.reset) after
	 * the server has fully stopped — must run after the save completes
	 * (ServerStopping fires before the save; clearing the static tables there
	 * would make the stop-save write an empty file, losing all AI towns and
	 * player-town simulations on restart).
	 *
	 * @param event 服务器停止完成事件 / the server stopped event
	 */
	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		AITownManager.reset();
	}

	/**
	 * 注册调试命令。
	 * <p>
	 * Registers debug commands.
	 *
	 * @param event 命令注册事件 / the command registration event
	 */
	@SubscribeEvent
	public static void onRegisterCommands(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("fhcitizen").requires(s -> s.hasPermission(2))
				.then(Commands.literal("spawn")
						.then(Commands.argument("count", IntegerArgumentType.integer(1, 20000))
								.executes(ctx -> spawn(ctx, 16))
								.then(Commands.argument("radius", IntegerArgumentType.integer(1, 64))
										.executes(ctx -> spawn(ctx, IntegerArgumentType.getInteger(ctx, "radius"))))))
				.then(Commands.literal("clear").executes(CitizenEvents::clear))
				.then(Commands.literal("count").executes(CitizenEvents::count))
				// 非玩家（AI）镇调试命令：创建测试镇 + 添加居民（居民模拟层对 holder 类型零假设）
				.then(Commands.literal("create_ai_town")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(CitizenEvents::createAITown)))
				.then(Commands.literal("ai_add_resident")
						.then(Commands.argument("firstName", StringArgumentType.word())
								.then(Commands.argument("lastName", StringArgumentType.word())
										.executes(CitizenEvents::aiAddResident)))));
	}

	/**
	 * 创建非玩家（AI）测试镇：走独立 Town 数据（AITownData）——不建队伍 holder、
	 * 不伪造 AbstractTeam；全局单文件（AITownManager）持久化，维度落盘在本数据上
	 * （调度器门控依据）。不参与每日结算（调试镇稳定不演化）。
	 * <p>
	 * Creates a non-player (AI) test town: an independent Town (AITownData) —
	 * no team holder, no fake AbstractTeam; persisted by AITownManager's global
	 * single-file save, with the dimension stored on the data itself (the
	 * scheduler gate). Skips the daily settlement (stable debug town).
	 */
	private static int createAITown(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String name = StringArgumentType.getString(ctx, "name");
		AITownData town = AITownManager.getOrCreate(name, player.level().dimension());
		ctx.getSource().sendSuccess(() -> Component.literal("AI town '" + name + "' created (dimension "
				+ player.level().dimension().location() + ")"), true);
		return 1;
	}

	/**
	 * 向最近创建的 AI 镇添加一名居民（出生锚点 = 玩家当前位置）。
	 * AITownData.addResident 直调模拟事件 → 条目立即在锚点附近出生
	 * （未接管时由下次注册表聚合的接管对账补建）。
	 * <p>
	 * Adds a resident to the most recently created AI town (spawn anchor =
	 * the player's position). AITownData.addResident calls the sim event
	 * directly → the entry spawns near the anchor immediately (or at the
	 * next takeover reconciliation if not yet adopted).
	 */
	private static int aiAddResident(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		AITownData town = AITownManager.lastAI();
		if (town == null) {
			ctx.getSource().sendSuccess(
					() -> Component.literal("No AI town yet: use /fhcitizen create_ai_town <name> first"), false);
			return 0;
		}
		String first = StringArgumentType.getString(ctx, "firstName");
		String last = StringArgumentType.getString(ctx, "lastName");
		Resident resident = new Resident(first, last);
		resident.setHousePos(player.blockPosition());
		town.addResident(resident);
		ctx.getSource().sendSuccess(() -> Component.literal("Added resident " + first + " " + last + " to AI town '"
				+ town.getName() + "'"), true);
		return 1;
	}

	private static int spawn(CommandContext<CommandSourceStack> ctx, int radius)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = player.serverLevel();
		CitizenSimScheduler sched = CitizenSimScheduler.get(level);
		int count = IntegerArgumentType.getInteger(ctx, "count");
		int cx = player.getBlockX();
		int cz = player.getBlockZ();
		java.util.Random rand = new java.util.Random();
		for (int i = 0; i < count; i++) {
			double angle = rand.nextDouble() * Math.PI * 2;
			double dist = Math.sqrt(rand.nextDouble()) * radius;
			sched.spawnUnmanaged(level, cx + (int) Math.round(Math.cos(angle) * dist),
					cz + (int) Math.round(Math.sin(angle) * dist));
		}
		final int total = sched.countAll();
		ctx.getSource().sendSuccess(() -> Component.literal("Spawned " + count + " citizens, total " + total), true);
		return count;
	}

	private static int clear(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerLevel level = player.serverLevel();
		// 语义修正：只清未托管命令居民——镇居民是 town 驱动的模拟，调试命令不碰
		// (Semantics fix: clears only unmanaged command citizens — town residents
		// are town-driven and untouched by this debug command.)
		int cleared = CitizenSimScheduler.get(level).clearUnmanaged(level);
		ctx.getSource().sendSuccess(() -> Component.literal("Cleared " + cleared + " unmanaged citizens"), true);
		return cleared;
	}

	private static int count(CommandContext<CommandSourceStack> ctx)
			throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		int total = CitizenSimScheduler.get(player.serverLevel()).countAll();
		ctx.getSource().sendSuccess(() -> Component.literal("Citizens in this dimension: " + total), false);
		return total;
	}
}

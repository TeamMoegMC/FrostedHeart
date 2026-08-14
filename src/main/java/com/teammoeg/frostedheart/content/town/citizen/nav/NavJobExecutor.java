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

package com.teammoeg.frostedheart.content.town.citizen.nav;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 寻路任务执行器：守护线程池跑异步构建，完成回调经并发队列回主线程消化。
 * 线程数 = 核数 - 1（至少 1）；懒启动，服务器停止时 {@link #shutdown()}。
 * 主线程零阻塞：提交与安装都是 O(1)。
 * <p>
 * Pathfinding job executor: a daemon thread pool runs async builds while
 * completion callbacks are handed back to the main thread through a concurrent
 * queue. Thread count = cores - 1 (at least 1); lazily started, shut down on
 * server stop via {@link #shutdown()}. The main thread never blocks: both
 * submission and installation are O(1).
 */
public final class NavJobExecutor {

	private static volatile ExecutorService pool;
	private static final Queue<Runnable> MAIN_TASKS = new ConcurrentLinkedQueue<>();

	private NavJobExecutor() {
	}

	private static synchronized ExecutorService pool() {
		ExecutorService p = pool;
		if (p == null || p.isShutdown()) {
			int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
			p = Executors.newFixedThreadPool(threads, r -> {
				Thread t = new Thread(r, "fh-citizen-nav");
				t.setDaemon(true);
				return t;
			});
			pool = p;
		}
		return p;
	}

	/**
	 * 提交异步任务。任务内可调用 {@link #toMain(Runnable)} 安排主线程回调。
	 * <p>
	 * Submits an async job. The job may schedule main-thread callbacks via
	 * {@link #toMain(Runnable)}.
	 *
	 * @param job 异步任务 / the async job
	 */
	public static void submit(Runnable job) {
		pool().submit(job);
	}

	/**
	 * 安排一个主线程回调（由异步任务调用）。
	 * <p>
	 * Schedules a main-thread callback (called from async jobs).
	 *
	 * @param task 主线程回调 / the main-thread callback
	 */
	public static void toMain(Runnable task) {
		MAIN_TASKS.add(task);
	}

	/**
	 * 主线程每 tick 调用，消化最多 max 个完成回调（限个数防尖峰）。
	 * <p>
	 * Called on the main thread every tick; drains at most max completion
	 * callbacks (capped to prevent spikes).
	 *
	 * @param max 本 tick 最多消化的回调数 / max callbacks to drain this tick
	 */
	public static void drainMain(int max) {
		for (int k = 0; k < max; k++) {
			Runnable r = MAIN_TASKS.poll();
			if (r == null)
				return;
			try {
				r.run();
			} catch (Throwable t) {
				// 回调失败不拖垮主 tick / a failing callback must not break the main tick
				t.printStackTrace();
			}
		}
	}

	/**
	 * 关闭线程池并清空待消化回调（服务器停止时调用）。
	 * <p>
	 * Shuts down the pool and clears pending callbacks (called on server stop).
	 */
	public static synchronized void shutdown() {
		ExecutorService p = pool;
		if (p != null)
			p.shutdownNow();
		pool = null;
		MAIN_TASKS.clear();
	}
}

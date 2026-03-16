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

/**
 * 调度器包。提供基于tick的延迟任务调度系统，
 * 允许方块实体注册定时执行的任务，由全局队列统一调度。
 * <p>
 * Scheduler package. Provides a tick-based delayed task scheduling system,
 * allowing block entities to register timed tasks for execution,
 * managed by a global queue.
 *
 * @see com.teammoeg.chorda.scheduler.SchedulerQueue
 * @see com.teammoeg.chorda.scheduler.ScheduledTaskTileEntity
 */
package com.teammoeg.chorda.scheduler;

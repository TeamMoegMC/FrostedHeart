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

package com.teammoeg.frostedheart.content.robotics.logistics.tasks;

import com.mojang.serialization.Codec;
import com.teammoeg.chorda.io.registry.TypedCodecRegistry;
import com.teammoeg.frostedheart.content.robotics.logistics.LogisticNetwork;
/**
 * Stage for tasks:
 * Queue->Prepared->Working->finished
 *            ^-------|
 * 
 * */
public abstract class LogisticTask {

	public int ticks;
	public LogisticTaskKey taskKey;
	public LogisticTask(LogisticTaskKey taskKey, int ticks) {
		super();
		this.taskKey = taskKey;
		this.ticks = ticks;
	}
	public LogisticTask() {
		super();
	}
	public abstract LogisticTask prepare(LogisticNetwork network);
	public abstract LogisticTask work(LogisticNetwork network);
}

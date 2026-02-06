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

package com.teammoeg.frostedheart.content.scenario.runner;

public enum RunStatus {
	RUNNING(false,true),//running 
	WAITCLIENT(false,true),//waiting for client response
	WAITACTION(false,false),//waiting for player action
	WAITTIMER,//waiting for timer
	WAITNETWORK,//waiting for network
	WAITTRIGGER(true,false,true),//waiting for async trigger
	PAUSED(true,false,true),//Paused from use action
	STOPPED(true,false,true),//stopped
	WAITTRANS,//waiting for transition
	WAITRENDER//waiting for render
	;
	public final boolean doPersist;
	public final boolean shouldRun;
	public final boolean shouldPause;

	RunStatus(boolean doPersist, boolean shouldRun, boolean shouldPause) {
		this.doPersist = doPersist;
		this.shouldRun = shouldRun;
		this.shouldPause = shouldPause;
	}
	RunStatus(boolean doPersist, boolean shouldRun) {
		this.doPersist = doPersist;
		this.shouldRun = shouldRun;
		this.shouldPause = false;
	}

	RunStatus() {
		this.doPersist = false;
		this.shouldRun = false;
		this.shouldPause=false;
	}
}

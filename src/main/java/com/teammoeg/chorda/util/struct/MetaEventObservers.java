/*
 * Copyright (c) 2024 TeamMoeg
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

package com.teammoeg.chorda.util.struct;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
/**
 * Registry for "meta" events.
 * meta event is a flag that changes after specific initialization code is executed.
 * Listeners would be called after the flag changes, or immediate when flag is already changed.
 * 
 * */
public class MetaEventObservers {
	List<Supplier<Runnable>> listener=new ArrayList<>();
	boolean isFired=false;
	public MetaEventObservers() {
	}
	public synchronized void addListener(Supplier<Runnable> runnable) {
		if(isFired) {
			runnable.get().run();
		}else 
			listener.add(runnable);
		
	}
	public synchronized void setFinished() {
		isFired=true;
		listener.forEach(t->t.get().run());
	}

}

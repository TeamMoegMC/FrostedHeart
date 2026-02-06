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

package com.teammoeg.chorda.util.struct;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
/**
 * observer listeners for a flag
 * Listeners would be called after the flag changes, or immediate when flag is already changed.
 * 
 * */
public class BitObserverList {
	List<Supplier<Runnable>> listener=new ArrayList<>();
	boolean isFired=false;
	public BitObserverList() {
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
		listener.clear();
	}
	public synchronized void resetFinished() {
		isFired=false;
		
	}
}

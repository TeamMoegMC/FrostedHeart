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
 * 标志位观察者列表，监听器在标志变化后调用，如果标志已经改变则立即调用。
 * 线程安全的实现，适合跨线程通知场景。
 * <p>
 * Observer listener list for a flag. Listeners are called after the flag changes,
 * or immediately if the flag has already changed. Thread-safe implementation
 * suitable for cross-thread notification scenarios.
 */
public class BitObserverList {
	List<Supplier<Runnable>> listener=new ArrayList<>();
	boolean isFired=false;
	public BitObserverList() {
	}
	/**
	 * 添加监听器。如果标志已触发，则立即执行；否则加入等待列表。
	 * <p>
	 * Add a listener. If the flag has already fired, execute immediately;
	 * otherwise add to the waiting list.
	 *
	 * @param runnable 监听器供应器，get()返回的Runnable将在触发时执行 / the listener supplier whose get() Runnable will be executed on fire
	 */
	public synchronized void addListener(Supplier<Runnable> runnable) {
		if(isFired) {
			runnable.get().run();
		}else 
			listener.add(runnable);
		
	}
	/**
	 * 设置标志为已完成状态，触发所有等待中的监听器并清空列表。
	 * <p>
	 * Set the flag to finished state, fire all pending listeners and clear the list.
	 */
	public synchronized void setFinished() {
		isFired=true;
		listener.forEach(t->t.get().run());
		listener.clear();
	}
	/**
	 * 重置标志为未完成状态。
	 * <p>
	 * Reset the flag to unfinished state.
	 */
	public synchronized void resetFinished() {
		isFired=false;
		
	}
}

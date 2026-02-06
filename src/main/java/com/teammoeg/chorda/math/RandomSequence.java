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

package com.teammoeg.chorda.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
/**
 * Random generate values in a sequence, each number would only exist once per round
 * if all number is used, it would be reloaded.
 * 
 * */
public class RandomSequence {
	LinkedList<Integer> llist=new LinkedList<>();
	List<Integer> ilist=new ArrayList<>();
	Random rnd;
	public RandomSequence(int maxLen,Random rnd) {
		for(int i=0;i<maxLen;i++) {
			ilist.add(i);
		}
		this.rnd=rnd;
	}
	public RandomSequence(List<Integer> initialSequence,Random rnd) {
		ilist.addAll(initialSequence);
		this.rnd=rnd;
	}
	public int getNext() {
		if(llist.isEmpty()) {
			Collections.shuffle(ilist, rnd);
			llist.addAll(ilist);
		}
		return llist.poll();
		
	}
	public void clear() {
		llist.clear();
	}

}

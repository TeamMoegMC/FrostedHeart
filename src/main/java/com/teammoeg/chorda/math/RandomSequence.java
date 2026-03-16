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
 * 随机序列生成器，每轮中每个数字只出现一次。当所有数字用完后，自动重新洗牌并重新加载。
 * <p>
 * Random sequence generator where each number appears only once per round.
 * When all numbers are used, the sequence is automatically reshuffled and reloaded.
 */
public class RandomSequence {
	LinkedList<Integer> llist=new LinkedList<>();
	List<Integer> ilist=new ArrayList<>();
	Random rnd;
	/**
	 * 构造一个包含从0到maxLen-1的整数序列的随机序列生成器。
	 * <p>
	 * Constructs a random sequence generator containing integers from 0 to maxLen-1.
	 *
	 * @param maxLen 序列长度（最大值+1） / the sequence length (maximum value + 1)
	 * @param rnd 用于洗牌的随机数生成器 / the random number generator for shuffling
	 */
	public RandomSequence(int maxLen,Random rnd) {
		for(int i=0;i<maxLen;i++) {
			ilist.add(i);
		}
		this.rnd=rnd;
	}
	/**
	 * 使用指定的初始序列构造随机序列生成器。
	 * <p>
	 * Constructs a random sequence generator with the specified initial sequence.
	 *
	 * @param initialSequence 初始整数序列 / the initial integer sequence
	 * @param rnd 用于洗牌的随机数生成器 / the random number generator for shuffling
	 */
	public RandomSequence(List<Integer> initialSequence,Random rnd) {
		ilist.addAll(initialSequence);
		this.rnd=rnd;
	}
	/**
	 * 获取序列中的下一个随机值。如果当前轮次已用完，则自动重新洗牌。
	 * <p>
	 * Gets the next random value in the sequence. Automatically reshuffles when the current round is exhausted.
	 *
	 * @return 下一个随机值 / the next random value
	 */
	public int getNext() {
		if(llist.isEmpty()) {
			Collections.shuffle(ilist, rnd);
			llist.addAll(ilist);
		}
		return llist.poll();
		
	}
	/**
	 * 清空当前轮次的剩余序列，下次调用getNext时将重新洗牌。
	 * <p>
	 * Clears the remaining sequence of the current round; the next call to getNext will reshuffle.
	 */
	public void clear() {
		llist.clear();
	}

}

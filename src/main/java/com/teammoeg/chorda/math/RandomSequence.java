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

package com.teammoeg.frostedheart.content.robotics.logistics;

import java.util.LinkedHashSet;
import java.util.LinkedList;

import com.mojang.datafixers.util.Pair;

import net.minecraft.item.ItemStack;

public class LogisticList {
	LinkedList<Pair<ItemStack,LinkedHashSet<LogisticSlot>>> list=new LinkedList<>();
	LinkedHashSet<LogisticSlot> deflist;
	public LogisticList() {
		// TODO Auto-generated constructor stub
	}

}

package com.teammoeg.frostedheart.trade;

import java.util.Map;

import net.minecraft.item.ItemStack;

public class SellData {
	ItemStack item;
	int store;
	String id;
	public SellData(ItemStack item, int store, String id) {
		super();
		this.item = item;
		this.store = store;
		this.id = id;
	}
	public ItemStack getItem() {
		return item;
	}
	public int getStore() {
		return store;
	}
	public boolean reduceStock(Map<String,Double> d,int count) {
		return d.computeIfPresent(id, (k,v)->v-count)!=null;
	}
}

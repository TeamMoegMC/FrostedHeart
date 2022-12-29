package com.teammoeg.frostedheart.trade;

import java.util.Map;

import net.minecraft.item.crafting.Ingredient;

public class BuyData {
	Ingredient item;
	int store;
	String id;
	public BuyData(Ingredient item, int store, String id) {
		super();
		this.item = item;
		this.store = store;
		this.id = id;
	}
	public Ingredient getItem() {
		return item;
	}
	public int getStore() {
		return store;
	}
	public boolean reduceStock(Map<String,Float> d,int count) {
		return d.computeIfPresent(id, (k,v)->v-count)!=null;
	}
}

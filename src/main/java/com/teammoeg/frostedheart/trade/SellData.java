package com.teammoeg.frostedheart.trade;

import java.util.Map;

import net.minecraft.item.ItemStack;

public class SellData {
	ItemStack item;
	int store;
	String id;
	int price;

	public SellData(ItemStack item, int store, String id, int price) {
		super();
		this.item = item;
		this.store = store;
		this.id = id;
		this.price = price;
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
	@Override
	public String toString() {
		return "SellData [item=" + item + ", store=" + store + ", id=" + id + ", price=" + price + "]";
	}
}

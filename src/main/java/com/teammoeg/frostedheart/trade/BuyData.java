package com.teammoeg.frostedheart.trade;

import net.minecraft.item.crafting.Ingredient;

public class BuyData {
	
	String id;
	int store;
	DemandData bd;

	public BuyData(String id, int store,DemandData bd) {
		super();
		this.id = id;
		this.store = store;
		this.bd = bd;
	}
	public Ingredient getItem() {
		return bd.item;
	}
	public int getPrice() {
		return bd.price;
	}
	public boolean isFullStock() {
		return store>=bd.maxstore;
	}
	public boolean canRestock(FHVillagerData data) {
		return bd.canRestock(data);
	}
	public int getStore() {
		return store;
	}
	public void reduceStock(FHVillagerData data,int count) {
		bd.soldactions.forEach(c->c.deal(data, count));
		data.storage.computeIfPresent(id, (k,v)->v-count);
	}
}

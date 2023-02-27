package com.teammoeg.frostedheart.trade.policy.snapshot;

import com.teammoeg.frostedheart.trade.FHVillagerData;
import com.teammoeg.frostedheart.trade.policy.ProductionData;

import net.minecraft.item.ItemStack;

public class SellData {
	String id;
	int store;
	
	ProductionData data;

	public SellData(String id, int store, ProductionData data) {
		super();
		this.id = id;
		this.store = store;
		this.data = data;
	}
	public ItemStack getItem() {
		return data.item;
	}
	public int getStore() {
		return store;
	}
	public int getPrice() {
		return data.price;
	}
	public boolean canRestock(FHVillagerData data) {
		
		return this.data.canRestock(data);
	}
	public boolean isFullStock() {
		return store>=data.maxstore;
	}
	public void reduceStock(FHVillagerData data,int count) {
		data.storage.computeIfPresent(getId(), (k,v)->v-count);
		this.data.soldactions.forEach(c->c.deal(data, count));
	}
	public String getId() {
		return id;
	}

}

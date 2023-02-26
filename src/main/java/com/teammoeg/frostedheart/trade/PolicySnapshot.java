package com.teammoeg.frostedheart.trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolicySnapshot {
	public static final PolicySnapshot empty=new PolicySnapshot() {
		@Override
		public void register(BaseData bd) {}
		@Override
		public void calculateRecovery(int deltaDays,FHVillagerData data) {}
		@Override
		public void fetchTrades(Map<String, Float> data) {}
	};
	Map<String,BaseData> data=new HashMap<>();
	List<BuyData> buys=new ArrayList<>();
	Map<String,SellData> sells=new HashMap<>();
	public void register(BaseData bd) {
		data.put(bd.getId(), bd);
	}
	public void calculateRecovery(int deltaDays,FHVillagerData data) {
		System.out.println(data);
		this.data.values().forEach(t->t.tick(deltaDays, data));
		System.out.println(data);
		
	}
	public void fetchTrades(Map<String,Float> data){
		this.data.values().forEach(t->t.fetch(this,data));
	}
	public void registerBuy(BuyData bd) {
		buys.add(bd);
	}
	public void registerSell(SellData sd) {
		sells.put(sd.id, sd);
	}
	@Override
	public String toString() {
		return "PolicySnapshot [data=" + data + ", buys=" + buys + ", sells=" + sells + "]";
	}
}

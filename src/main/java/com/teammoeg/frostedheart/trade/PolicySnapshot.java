package com.teammoeg.frostedheart.trade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolicySnapshot {
	Map<String,BaseData> data=new HashMap<>();
	List<BuyData> buys=new ArrayList<>();
	List<SellData> sells=new ArrayList<>();
	public void register(BaseData bd) {
		data.put(bd.getId(), bd);
	}
	public void calculateRecovery(int deltaDays,Map<String,Float> data) {
		this.data.values().forEach(t->t.tick(deltaDays, data));
	}
	public void fetchTrades(Map<String,Float> data){
		this.data.values().forEach(t->t.fetch(buys, sells,data));
	}
}

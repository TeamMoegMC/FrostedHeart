package com.teammoeg.frostedheart.trade;

import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.teammoeg.frostedheart.util.Writeable;

import net.minecraft.network.PacketBuffer;

public abstract class BaseData implements Writeable{
	private String id;
	int maxstore;
	float recover;
	int price;
	
	public BaseData(String id, int maxstore, float recover, int price) {
		super();
		this.id = id;
		this.maxstore = maxstore;
		this.recover = recover;
		this.price = price;
	}
	public BaseData(JsonObject jo) {
		id=jo.get("id").getAsString();
		maxstore=jo.get("store").getAsInt();
		recover=jo.get("recover").getAsFloat();
		price=jo.get("price").getAsInt();
	}
	public BaseData(PacketBuffer pb) {
		id=pb.readString();
		maxstore=pb.readVarInt();
		recover=pb.readFloat();
		price=pb.readVarInt();
	}
	
	public void tick(int deltaDay,Map<String,Float> data) {
		if(deltaDay>0) 
			data.compute(id,(k,v)->v==null?recover*deltaDay:Math.min(recover*deltaDay+v,maxstore));
	}
	public abstract String getType();
	public String getId() {
		return id+"_"+getType();
	}
	public abstract void fetch(List<BuyData> buys,List<SellData> sell,Map<String,Float> data);
	@Override
	public JsonElement serialize() {
		JsonObject jo=new JsonObject();
		jo.addProperty("id", id);
		jo.addProperty("store", maxstore);
		jo.addProperty("recover",recover);
		jo.addProperty("price", price);
		return jo;
	}
	@Override
	public void write(PacketBuffer buffer) {
		buffer.writeString(id);
		buffer.writeVarInt(maxstore);
		buffer.writeFloat(recover);
		buffer.writeVarInt(price);
	}
	public static BaseData read(PacketBuffer pb) {
		if(pb.readBoolean())
			return new ProductionData(pb);
		return new DemandData(pb);
	}
	public static BaseData read(JsonObject jo) {
		if(jo.has("produce"))
			return new ProductionData(jo);
		else if(jo.has("demand"))
			return new DemandData(jo);
		throw new IllegalArgumentException("Missing produce or demand field");
			
	}
}

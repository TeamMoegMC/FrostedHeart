package com.teammoeg.frostedheart.research.number;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.teammoeg.frostedheart.research.data.ResearchData;
import com.teammoeg.frostedheart.util.Writeable;

import net.minecraft.network.PacketBuffer;

public class ConstResearchNumber implements IResearchNumber,Writeable{
	private static Cache<Number,ConstResearchNumber> cb=CacheBuilder.newBuilder().expireAfterAccess(20, TimeUnit.SECONDS).build();
	final Number n;
	public static ConstResearchNumber valueOf(Number n) {
		try {
			return cb.get(n,()-> new ConstResearchNumber(n));
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	public ConstResearchNumber(Number n) {
		super();
		this.n = n;
		
	}
	public static ConstResearchNumber valueOf(PacketBuffer buffer) {
		return valueOf(buffer.readDouble());
	}
	public static ConstResearchNumber valueOf(JsonObject buffer) {
		return valueOf(buffer.get("value").getAsNumber());
	}
	@Override
	public double getVal(ResearchData rd) {
		return n.doubleValue();
	}

	@Override
	public int getInt(ResearchData rd) {
		return n.intValue();
	}

	@Override
	public long getLong(ResearchData rd) {
		return n.longValue();
	}

	@Override
	public JsonElement serialize() {
		return new JsonPrimitive(n);
	}

	@Override
	public void write(PacketBuffer buffer) {
		buffer.writeDouble(n.doubleValue());
	}

}

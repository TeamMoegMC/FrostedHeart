package com.teammoeg.frostedheart.research.number;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.teammoeg.frostedheart.research.data.ResearchData;
import com.teammoeg.frostedheart.util.Calculator;
import com.teammoeg.frostedheart.util.Writeable;

import net.minecraft.network.PacketBuffer;

public class ExpResearchNumber implements IResearchNumber, Writeable {
	Calculator.Node calc;
	String exp;
	public ExpResearchNumber(String exp) {
		super();
		this.exp = exp;
		calc=Calculator.eval(exp);
	}
	public ExpResearchNumber(PacketBuffer buffer) {
		this(buffer.readString());
	}
	public ExpResearchNumber(JsonObject buffer) {
		this(buffer.get("exp").getAsString());
	}
	@Override
	public JsonElement serialize() {
		return new JsonPrimitive(exp);
	}

	@Override
	public void write(PacketBuffer buffer) {
		buffer.writeString(exp);
	}

	@Override
	public double getVal(ResearchData rd) {
		return calc.eval(rd);
	}

}

package com.teammoeg.frostedheart.data;

import com.google.gson.JsonObject;

public class BiomeTempData extends JsonDataHolder {

	public BiomeTempData(JsonObject data) {
		super(data);
	}
	public Float getTemp() {
		return this.getFloat("temperature");
	}
}

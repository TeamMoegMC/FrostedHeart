package com.teammoeg.frostedheart.trade;

import com.teammoeg.frostedheart.client.util.GuiUtils;

import net.minecraft.util.text.ITextComponent;

public enum RelationModifier {
	KILLED_HISTORY("history_killed"),
	UNKNOWN_LANGUAGE("unknown_language"),
	CHARM("charm"),
	KILLED_SAW("saw_murder"),
	HURT("hurt"),
	RECENT_BENEFIT("beneficial_trade"),
	TRADE_LEVEL("total_trade"),
	SAVED_VILLAGE("hero_village"),
	RECENT_BARGAIN("recent_bargain"),
	FOREIGNER("foreigner")
	;
	public final String tkey;

	private RelationModifier(String tkey) {
		this.tkey = tkey;
	}
	
	public ITextComponent getDesc() {
		return GuiUtils.translateGui("trade.relation."+tkey);
	}
}
